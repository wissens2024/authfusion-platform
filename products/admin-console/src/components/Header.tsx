'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowRightOnRectangleIcon } from '@heroicons/react/24/outline';
import { clearAccessToken, getCurrentUser } from '@/lib/auth';

export default function Header() {
  const router = useRouter();
  const [user, setUser] = useState<{ sub: string; roles: string[]; email?: string } | null>(null);

  useEffect(() => {
    setUser(getCurrentUser());
  }, []);

  const handleLogout = () => {
    clearAccessToken();
    router.push('/login');
  };

  return (
    <header className="bg-white border-b border-gray-200 h-16 flex items-center justify-between px-6">
      <div />
      <div className="flex items-center gap-4">
        {user && (
          <span className="text-sm text-gray-600">
            {user.sub}
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
