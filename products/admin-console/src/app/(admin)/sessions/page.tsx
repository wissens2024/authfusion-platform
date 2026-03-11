'use client';

import { useState, useEffect, useCallback } from 'react';
import LoadingSpinner from '@/components/LoadingSpinner';
import { sessionApi } from '@/lib/api';
import type { Session } from '@/lib/types';

export default function SessionsPage() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);

  const loadSessions = useCallback(async () => {
    try {
      const { data } = await sessionApi.list();
      setSessions(Array.isArray(data) ? data : []);
    } catch {
      setSessions([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadSessions(); }, [loadSessions]);

  const handleRevoke = async (sessionId: string) => {
    if (!confirm('이 세션을 종료하시겠습니까?')) return;
    try {
      await sessionApi.revoke(sessionId);
      loadSessions();
    } catch { /* ignore */ }
  };

  if (loading) return <LoadingSpinner className="mt-20" />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">세션 관리</h2>
        <span className="text-sm text-gray-500">활성 세션: {sessions.length}</span>
      </div>

      <div className="card overflow-hidden p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-gray-600">세션 ID</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">사용자</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">IP</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">생성 시간</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">만료 시간</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {sessions.map((session) => (
              <tr key={session.sessionId} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-xs">{session.sessionId.substring(0, 12)}...</td>
                <td className="px-4 py-3">{session.username}</td>
                <td className="px-4 py-3 text-gray-500">{session.ipAddress || '-'}</td>
                <td className="px-4 py-3 text-gray-500 text-xs">
                  {new Date(session.createdAt).toLocaleString('ko-KR')}
                </td>
                <td className="px-4 py-3 text-gray-500 text-xs">
                  {new Date(session.expiresAt).toLocaleString('ko-KR')}
                </td>
                <td className="px-4 py-3 text-right">
                  <button onClick={() => handleRevoke(session.sessionId)}
                    className="text-red-600 hover:text-red-800 text-xs">세션 종료</button>
                </td>
              </tr>
            ))}
            {sessions.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">활성 세션 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
