'use client';

import { useState, useEffect } from 'react';
import StatusBadge from '@/components/StatusBadge';
import LoadingSpinner from '@/components/LoadingSpinner';
import { healthApi } from '@/lib/api';
import type { HealthStatus } from '@/lib/types';

export default function SettingsPage() {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [ssoUrl] = useState(process.env.NEXT_PUBLIC_SSO_SERVER_URL || 'http://localhost:8081');

  useEffect(() => {
    async function loadHealth() {
      try {
        const { data } = await healthApi.check();
        setHealth(data);
      } catch {
        setHealth({ status: 'DOWN' });
      } finally {
        setLoading(false);
      }
    }
    loadHealth();
  }, []);

  if (loading) return <LoadingSpinner className="mt-20" />;

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold">시스템 설정</h2>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* System Health */}
        <div className="card">
          <h3 className="font-semibold mb-4">시스템 상태</h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-sm">전체 상태</span>
              <StatusBadge status={health?.status || 'DOWN'} />
            </div>
            {health?.components && Object.entries(health.components).map(([name, comp]) => (
              <div key={name} className="flex justify-between items-center">
                <span className="text-sm text-gray-600 capitalize">{name}</span>
                <StatusBadge status={comp.status} />
              </div>
            ))}
          </div>
        </div>

        {/* Connection Info */}
        <div className="card">
          <h3 className="font-semibold mb-4">연결 정보</h3>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">SSO Server URL</span>
              <span className="font-mono text-xs">{ssoUrl}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">OIDC Discovery</span>
              <a href={`${ssoUrl}/.well-known/openid-configuration`}
                target="_blank" rel="noopener noreferrer"
                className="text-primary-600 hover:underline text-xs">열기</a>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">JWKS</span>
              <a href={`${ssoUrl}/.well-known/jwks.json`}
                target="_blank" rel="noopener noreferrer"
                className="text-primary-600 hover:underline text-xs">열기</a>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Swagger UI</span>
              <a href={`${ssoUrl}/swagger-ui.html`}
                target="_blank" rel="noopener noreferrer"
                className="text-primary-600 hover:underline text-xs">열기</a>
            </div>
          </div>
        </div>

        {/* OIDC Endpoints */}
        <div className="card lg:col-span-2">
          <h3 className="font-semibold mb-4">OIDC 엔드포인트</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
            {[
              { label: 'Authorization', path: '/oauth2/authorize' },
              { label: 'Token', path: '/oauth2/token' },
              { label: 'UserInfo', path: '/oauth2/userinfo' },
              { label: 'Revocation', path: '/oauth2/revoke' },
              { label: 'JWKS', path: '/.well-known/jwks.json' },
              { label: 'Discovery', path: '/.well-known/openid-configuration' },
            ].map(({ label, path }) => (
              <div key={path} className="flex justify-between items-center bg-gray-50 rounded-lg px-3 py-2">
                <span className="text-gray-600">{label}</span>
                <span className="font-mono text-xs text-gray-400">{path}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
