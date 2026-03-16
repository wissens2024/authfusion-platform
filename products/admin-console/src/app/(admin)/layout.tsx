'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Sidebar from '@/components/Sidebar';
import Header from '@/components/Header';
import { isAuthenticated, isAdmin, getTokenRemainingMs, clearAuth } from '@/lib/auth';

const SESSION_WARNING_MS = 60_000; // 만료 1분 전 경고

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);
  const [sessionWarning, setSessionWarning] = useState(false);

  const checkAuth = useCallback(() => {
    // 인증 여부 확인 (FIA_UAU.1 - 토큰 만료 포함)
    if (!isAuthenticated()) {
      clearAuth();
      router.push('/login');
      return;
    }
    // ADMIN 역할 확인 (FDP_ACC.1 - 접근 제어)
    if (!isAdmin()) {
      clearAuth();
      router.push('/login');
      return;
    }
    setAuthorized(true);
  }, [router]);

  useEffect(() => {
    checkAuth();

    // 주기적 세션 만료 검사 (30초마다)
    const interval = setInterval(() => {
      const remaining = getTokenRemainingMs();
      if (remaining <= 0) {
        clearAuth();
        router.push('/login');
        return;
      }
      if (remaining <= SESSION_WARNING_MS) {
        setSessionWarning(true);
      }
    }, 30_000);

    return () => clearInterval(interval);
  }, [checkAuth, router]);

  // 권한 확인 전까지 빈 화면 (깜빡임 방지 + 미인가 콘텐츠 노출 방지)
  if (!authorized) return null;

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex-1 flex flex-col">
        <Header />
        {sessionWarning && (
          <div className="bg-yellow-50 border-b border-yellow-200 px-6 py-2 text-sm text-yellow-800">
            세션이 곧 만료됩니다. 다시 로그인해 주세요.
          </div>
        )}
        <main className="flex-1 p-6 overflow-auto">{children}</main>
      </div>
    </div>
  );
}
