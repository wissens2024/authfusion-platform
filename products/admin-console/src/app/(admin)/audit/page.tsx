'use client';

import { useState, useEffect, useCallback } from 'react';
import { MagnifyingGlassIcon } from '@heroicons/react/24/outline';
import LoadingSpinner from '@/components/LoadingSpinner';
import { auditApi } from '@/lib/api';
import type { AuditEvent } from '@/lib/types';

const EVENT_TYPES = [
  '', 'AUTHENTICATION', 'USER_MANAGEMENT', 'CLIENT_MANAGEMENT', 'TOKEN_OPERATION',
];

const ACTION_LABELS: Record<string, string> = {
  LOGIN_SUCCESS: '로그인 성공',
  LOGIN_FAILED: '로그인 실패',
  LOGIN_MFA_REQUIRED: 'MFA 필요',
  MFA_SUCCESS: 'MFA 성공',
  MFA_FAILED: 'MFA 실패',
  LOGOUT: '로그아웃',
  USER_CREATED: '사용자 생성',
  USER_UPDATED: '사용자 수정',
  USER_DELETED: '사용자 삭제',
  CLIENT_CREATED: '클라이언트 생성',
  CLIENT_DELETED: '클라이언트 삭제',
  TOKEN_ISSUED: '토큰 발급',
  TOKEN_REVOKED: '토큰 폐기',
};

export default function AuditPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
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
      setEvents(data.content ?? []);
      setTotalElements(data.totalElements ?? 0);
    } catch {
      setEvents([]);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [eventType, username]);

  useEffect(() => { loadEvents(); }, [loadEvents]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">감사 로그</h2>
        <span className="text-sm text-gray-500">총 {totalElements.toLocaleString()}건</span>
      </div>

      <div className="flex gap-3">
        <select value={eventType} onChange={(e) => setEventType(e.target.value)} className="input-field w-52">
          <option value="">전체 이벤트 유형</option>
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
        <button onClick={loadEvents} className="btn-secondary px-4">새로고침</button>
      </div>

      {loading ? (
        <LoadingSpinner className="mt-10" />
      ) : (
        <div className="card overflow-hidden p-0">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-600">결과</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">유형</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">액션</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">사용자</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">IP</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">오류</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">시간</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {events.map((event) => (
                <tr key={event.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <span className={`w-2.5 h-2.5 rounded-full inline-block ${event.success ? 'bg-green-500' : 'bg-red-500'}`} />
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{event.eventType}</td>
                  <td className="px-4 py-3 font-mono text-xs">
                    {ACTION_LABELS[event.action] ? (
                      <span title={event.action}>{ACTION_LABELS[event.action]}</span>
                    ) : (
                      event.action
                    )}
                  </td>
                  <td className="px-4 py-3">{event.username || '-'}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{event.ipAddress || '-'}</td>
                  <td className="px-4 py-3 text-red-500 text-xs max-w-xs truncate">{event.errorMessage || '-'}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs whitespace-nowrap">
                    {new Date(event.timestamp).toLocaleString('ko-KR')}
                  </td>
                </tr>
              ))}
              {events.length === 0 && (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">이벤트 없음</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
