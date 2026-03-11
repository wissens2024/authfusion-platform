'use client';

import { useState, useEffect, useCallback } from 'react';
import { MagnifyingGlassIcon } from '@heroicons/react/24/outline';
import LoadingSpinner from '@/components/LoadingSpinner';
import { auditApi } from '@/lib/api';
import type { AuditEvent } from '@/lib/types';

const EVENT_TYPES = [
  '', 'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT', 'TOKEN_ISSUED', 'TOKEN_REVOKED',
  'USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'CLIENT_CREATED', 'CLIENT_DELETED',
  'MFA_ENABLED', 'MFA_VERIFIED', 'PASSWORD_CHANGED', 'ACCOUNT_LOCKED',
];

export default function AuditPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [eventType, setEventType] = useState('');
  const [username, setUsername] = useState('');

  const loadEvents = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await auditApi.list({
        eventType: eventType || undefined,
        username: username || undefined,
        size: 100,
      });
      setEvents(Array.isArray(data) ? data : []);
    } catch {
      setEvents([]);
    } finally {
      setLoading(false);
    }
  }, [eventType, username]);

  useEffect(() => { loadEvents(); }, [loadEvents]);

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold">감사 로그</h2>

      <div className="flex gap-3">
        <select value={eventType} onChange={(e) => setEventType(e.target.value)} className="input-field w-48">
          <option value="">전체 이벤트</option>
          {EVENT_TYPES.filter(Boolean).map((type) => (
            <option key={type} value={type}>{type}</option>
          ))}
        </select>
        <div className="relative flex-1">
          <MagnifyingGlassIcon className="w-5 h-5 absolute left-3 top-2.5 text-gray-400" />
          <input
            type="text"
            placeholder="사용자명으로 검색..."
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="input-field pl-10"
          />
        </div>
      </div>

      {loading ? (
        <LoadingSpinner className="mt-10" />
      ) : (
        <div className="card overflow-hidden p-0">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-600">결과</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">이벤트</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">사용자</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">IP</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">상세</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">시간</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {events.map((event) => (
                <tr key={event.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <span className={`w-2.5 h-2.5 rounded-full inline-block ${event.success ? 'bg-green-500' : 'bg-red-500'}`} />
                  </td>
                  <td className="px-4 py-3 font-mono text-xs">{event.eventType}</td>
                  <td className="px-4 py-3">{event.username || '-'}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{event.ipAddress || '-'}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs max-w-xs truncate">{event.details || '-'}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs whitespace-nowrap">
                    {new Date(event.createdAt).toLocaleString('ko-KR')}
                  </td>
                </tr>
              ))}
              {events.length === 0 && (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">이벤트 없음</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
