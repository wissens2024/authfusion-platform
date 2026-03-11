'use client';

import { useState, useEffect, useCallback } from 'react';
import { PlusIcon, MagnifyingGlassIcon } from '@heroicons/react/24/outline';
import Modal from '@/components/Modal';
import StatusBadge from '@/components/StatusBadge';
import LoadingSpinner from '@/components/LoadingSpinner';
import { userApi } from '@/lib/api';
import type { User } from '@/lib/types';

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ username: '', email: '', password: '', firstName: '', lastName: '' });
  const [error, setError] = useState('');

  const loadUsers = useCallback(async () => {
    try {
      const { data } = await userApi.list({ search: search || undefined });
      setUsers(Array.isArray(data) ? data : []);
    } catch {
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => { loadUsers(); }, [loadUsers]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await userApi.create(form);
      setShowCreate(false);
      setForm({ username: '', email: '', password: '', firstName: '', lastName: '' });
      loadUsers();
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || '생성에 실패했습니다.');
    }
  };

  const handleDelete = async (id: number, username: string) => {
    if (!confirm(`'${username}' 사용자를 삭제하시겠습니까?`)) return;
    try {
      await userApi.delete(id);
      loadUsers();
    } catch { /* ignore */ }
  };

  if (loading) return <LoadingSpinner className="mt-20" />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">사용자 관리</h2>
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-1">
          <PlusIcon className="w-4 h-4" /> 사용자 추가
        </button>
      </div>

      <div className="relative">
        <MagnifyingGlassIcon className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
        <input
          type="text"
          placeholder="사용자명 또는 이메일 검색..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="input-field pl-10"
        />
      </div>

      <div className="card overflow-hidden p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-gray-600">사용자명</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">이메일</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">상태</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">소스</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">마지막 로그인</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-medium">{user.username}</td>
                <td className="px-4 py-3 text-gray-600">{user.email}</td>
                <td className="px-4 py-3"><StatusBadge status={user.status} /></td>
                <td className="px-4 py-3 text-gray-500">{user.userSource || 'LOCAL'}</td>
                <td className="px-4 py-3 text-gray-500 text-xs">
                  {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('ko-KR') : '-'}
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => handleDelete(user.id, user.username)}
                    className="text-red-600 hover:text-red-800 text-xs"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">사용자 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="사용자 추가">
        <form onSubmit={handleCreate} className="space-y-3">
          {error && <div className="bg-red-50 text-red-700 px-3 py-2 rounded text-sm">{error}</div>}
          <div>
            <label className="block text-sm font-medium mb-1">사용자명 *</label>
            <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} className="input-field" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">이메일 *</label>
            <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="input-field" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">비밀번호 *</label>
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} className="input-field" required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium mb-1">이름</label>
              <input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">성</label>
              <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} className="input-field" />
            </div>
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
