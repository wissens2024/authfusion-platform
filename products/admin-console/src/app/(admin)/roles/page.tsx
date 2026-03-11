'use client';

import { useState, useEffect, useCallback } from 'react';
import { PlusIcon } from '@heroicons/react/24/outline';
import Modal from '@/components/Modal';
import LoadingSpinner from '@/components/LoadingSpinner';
import { roleApi } from '@/lib/api';
import type { Role } from '@/lib/types';

export default function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: '', description: '' });
  const [error, setError] = useState('');

  const loadRoles = useCallback(async () => {
    try {
      const { data } = await roleApi.list();
      setRoles(Array.isArray(data) ? data : []);
    } catch {
      setRoles([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadRoles(); }, [loadRoles]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await roleApi.create(form);
      setShowCreate(false);
      setForm({ name: '', description: '' });
      loadRoles();
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || '생성에 실패했습니다.');
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`'${name}' 역할을 삭제하시겠습니까?`)) return;
    try {
      await roleApi.delete(id);
      loadRoles();
    } catch { /* ignore */ }
  };

  if (loading) return <LoadingSpinner className="mt-20" />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">역할 관리</h2>
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-1">
          <PlusIcon className="w-4 h-4" /> 역할 추가
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {roles.map((role) => (
          <div key={role.id} className="card">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold">{role.name}</h3>
                <p className="text-sm text-gray-500 mt-1">{role.description || '설명 없음'}</p>
                <p className="text-xs text-gray-400 mt-2">
                  생성: {new Date(role.createdAt).toLocaleDateString('ko-KR')}
                </p>
              </div>
              <button onClick={() => handleDelete(role.id, role.name)}
                className="text-red-500 hover:text-red-700 text-xs">삭제</button>
            </div>
          </div>
        ))}
        {roles.length === 0 && (
          <div className="col-span-full text-center text-gray-400 py-8">역할 없음</div>
        )}
      </div>

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="역할 추가">
        <form onSubmit={handleCreate} className="space-y-3">
          {error && <div className="bg-red-50 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          <div>
            <label className="block text-sm font-medium mb-1">역할명 *</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="input-field" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">설명</label>
            <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className="input-field" rows={2} />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={() => setShowCreate(false)} className="btn-secondary">취소</button>
            <button type="submit" className="btn-primary">생성</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
