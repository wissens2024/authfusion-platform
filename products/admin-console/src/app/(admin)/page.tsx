'use client';

import { useState, useEffect } from 'react';
import { UsersIcon, KeyIcon, ShieldCheckIcon, ClockIcon } from '@heroicons/react/24/outline';
import StatCard from '@/components/StatCard';
import StatusBadge from '@/components/StatusBadge';
import LoadingSpinner from '@/components/LoadingSpinner';
import { userApi, clientApi, sessionApi, auditApi, healthApi } from '@/lib/api';
import type { AuditEvent, HealthStatus } from '@/lib/types';

export default function DashboardPage() {
  const [stats, setStats] = useState({ users: 0, clients: 0, sessions: 0, events: 0 });
  const [recentEvents, setRecentEvents] = useState<AuditEvent[]>([]);
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadDashboard() {
      try {
        const [users, clients, sessions, events, healthRes] = await Promise.allSettled([
          userApi.list(),
          clientApi.list(),
          sessionApi.list(),
          auditApi.list({ size: 10 }),
          healthApi.check(),
        ]);

        const eventsPage = events.status === 'fulfilled' ? events.value.data : null;

        setStats({
          users: users.status === 'fulfilled' ? (users.value.data as unknown[]).length : 0,
          clients: clients.status === 'fulfilled' ? (clients.value.data as unknown[]).length : 0,
          sessions: sessions.status === 'fulfilled' ? (sessions.value.data as unknown[]).length : 0,
          events: eventsPage ? (eventsPage.totalElements ?? eventsPage.content?.length ?? 0) : 0,
        });

        if (eventsPage?.content) {
          setRecentEvents(eventsPage.content.slice(0, 10));
        }
        if (healthRes.status === 'fulfilled') {
          setHealth(healthRes.value.data);
        }
      } catch {
        // Dashboard loads best-effort
      } finally {
        setLoading(false);
      }
    }
    loadDashboard();
  }, []);

  if (loading) return <LoadingSpinner className="mt-20" />;

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold">대시보드</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="사용자" value={stats.users} icon={UsersIcon} color="blue" />
        <StatCard title="클라이언트" value={stats.clients} icon={KeyIcon} color="green" />
        <StatCard title="활성 세션" value={stats.sessions} icon={ClockIcon} color="yellow" />
        <StatCard title="감사 이벤트" value={stats.events} icon={ShieldCheckIcon} color="red" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Health Status */}
        <div className="card">
          <h3 className="text-sm font-semibold text-gray-700 mb-4">시스템 상태</h3>
          {health ? (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm">SSO Server</span>
                <StatusBadge status={health.status} />
              </div>
              {health.components && Object.entries(health.components).map(([name, comp]) => (
                <div key={name} className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">{name}</span>
                  <StatusBadge status={comp.status} />
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-400">연결 대기 중...</p>
          )}
        </div>

        {/* Recent Audit Events */}
        <div className="card">
          <h3 className="text-sm font-semibold text-gray-700 mb-4">최근 감사 이벤트</h3>
          {recentEvents.length > 0 ? (
            <div className="space-y-2">
              {recentEvents.map((event) => (
                <div key={event.id} className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2">
                    <span className={`w-2 h-2 rounded-full ${event.success ? 'bg-green-500' : 'bg-red-500'}`} />
                    <span className="text-gray-700">{event.action || event.eventType}</span>
                    {event.username && <span className="text-gray-400">({event.username})</span>}
                  </div>
                  <span className="text-xs text-gray-400">
                    {new Date(event.timestamp).toLocaleString('ko-KR')}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-400">이벤트 없음</p>
          )}
        </div>
      </div>
    </div>
  );
}
