'use client';

import { useState, useEffect, useCallback } from 'react';
import { PlusIcon } from '@heroicons/react/24/outline';
import Modal from '@/components/Modal';
import StatusBadge from '@/components/StatusBadge';
import LoadingSpinner from '@/components/LoadingSpinner';
import { clientApi } from '@/lib/api';
import type { Client } from '@/lib/types';

const isValidUrl = (url: string): boolean => {
  try {
    const parsed = new URL(url);
    return ['http:', 'https:'].includes(parsed.protocol);
  } catch {
    return false;
  }
};

export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({
    clientId: '', clientName: '', clientType: 'CONFIDENTIAL' as const,
    redirectUris: '', scopes: 'openid profile email', grantTypes: 'authorization_code',
    requirePkce: true,
  });
  const [error, setError] = useState('');

  const loadClients = useCallback(async () => {
    try {
      const { data } = await clientApi.list();
      setClients(Array.isArray(data) ? data : []);
    } catch {
      setClients([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadClients(); }, [loadClients]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const uris = form.redirectUris.split('\n').map(s => s.trim()).filter(Boolean);
    if (uris.length > 0 && !uris.every(isValidUrl)) {
      setError('유효하지 않은 Redirect URI가 포함되어 있습니다.');
      return;
    }
    try {
      await clientApi.create({
        clientId: form.clientId,
        clientName: form.clientName,
        clientType: form.clientType,
        redirectUris: uris,
        scopes: form.scopes.split(' ').filter(Boolean),
        grantTypes: form.grantTypes.split(' ').filter(Boolean),
        requirePkce: form.requirePkce,
      });
      setShowCreate(false);
      loadClients();
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 403) setError('권한이 없습니다.');
      else if (status === 409) setError('이미 존재하는 클라이언트입니다.');
      else setError('클라이언트 생성에 실패했습니다.');
    }
  };

  const handleDelete = async (id: number, clientId: string) => {
    if (!confirm(`'${clientId}' 클라이언트를 삭제하시겠습니까?`)) return;
    try {
      await clientApi.delete(id);
      loadClients();
    } catch { /* ignore */ }
  };

  if (loading) return <LoadingSpinner className="mt-20" />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">클라이언트 관리</h2>
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-1">
          <PlusIcon className="w-4 h-4" /> 클라이언트 추가
        </button>
      </div>

      <div className="card overflow-hidden p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Client ID</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">이름</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">유형</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Grant Types</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">상태</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {clients.map((client) => (
              <tr key={client.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-xs">{client.clientId}</td>
                <td className="px-4 py-3">{client.clientName}</td>
                <td className="px-4 py-3 text-gray-500 text-xs">{client.clientType}</td>
                <td className="px-4 py-3 text-gray-500 text-xs">{client.grantTypes?.join(', ')}</td>
                <td className="px-4 py-3"><StatusBadge status={client.status} /></td>
                <td className="px-4 py-3 text-right">
                  <button onClick={() => handleDelete(client.id, client.clientId)}
                    className="text-red-600 hover:text-red-800 text-xs">삭제</button>
                </td>
              </tr>
            ))}
            {clients.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">클라이언트 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="클라이언트 추가">
        <form onSubmit={handleCreate} className="space-y-3">
          {error && <div className="bg-red-50 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          <div>
            <label className="block text-sm font-medium mb-1">Client ID *</label>
            <input value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })} className="input-field" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">이름 *</label>
            <input value={form.clientName} onChange={(e) => setForm({ ...form, clientName: e.target.value })} className="input-field" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Redirect URIs (줄바꿈 구분)</label>
            <textarea value={form.redirectUris} onChange={(e) => setForm({ ...form, redirectUris: e.target.value })}
              className="input-field" rows={3} placeholder="http://localhost:3001/callback" />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Scopes (공백 구분)</label>
            <input value={form.scopes} onChange={(e) => setForm({ ...form, scopes: e.target.value })} className="input-field" />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Grant Types (공백 구분)</label>
            <input value={form.grantTypes} onChange={(e) => setForm({ ...form, grantTypes: e.target.value })} className="input-field" />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={form.requirePkce} onChange={(e) => setForm({ ...form, requirePkce: e.target.checked })} />
            PKCE 필수
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={() => setShowCreate(false)} className="btn-secondary">취소</button>
            <button type="submit" className="btn-primary">생성</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
