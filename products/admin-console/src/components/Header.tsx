'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowRightOnRectangleIcon } from '@heroicons/react/24/outline';
import { clearAuth, getCurrentUser } from '@/lib/auth';
import { authApi } from '@/lib/api';

export default function Header() {
  const router = useRouter();
  const [user, setUser] = useState<{ sub: string; username: string; roles: string[]; email?: string } | null>(null);

  useEffect(() => {
    setUser(getCurrentUser());
  }, []);

  const handleLogout = async () => {
    try {
      // 서버 세션 무효화 (FAU_GEN.1 - 로그아웃 감사 이벤트 생성)
      await authApi.logout();
    } catch {
      // 서버 호출 실패해도 로컬 세션은 정리
    } finally {
      clearAuth();
      router.push('/login');
    }
  };

  return (
    <header className="bg-white border-b border-gray-200 h-16 flex items-center justify-between px-6">
      <div />
      <div className="flex items-center gap-4">
        {user && (
          <span className="text-sm text-gray-600">
            {user.username}
            {user.roles.length > 0 && (
              <span className="ml-2 text-xs text-gray-400">({user.roles.join(', ')})</span>
            )}
          </span>
        )}
        <button
          onClick={handleLogout}
          className="flex items-center text-sm text-gray-500 hover:text-gray-700 transition-colors"
        >
          <ArrowRightOnRectangleIcon className="w-5 h-5 mr-1" />
          로그아웃
        </button>
      </div>
    </header>
  );
}
