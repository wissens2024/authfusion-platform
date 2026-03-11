'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  HomeIcon,
  UsersIcon,
  KeyIcon,
  ShieldCheckIcon,
  ClockIcon,
  DocumentTextIcon,
  Cog6ToothIcon,
} from '@heroicons/react/24/outline';
import clsx from 'clsx';

const navigation = [
  { name: '대시보드', href: '/', icon: HomeIcon },
  { name: '사용자', href: '/users', icon: UsersIcon },
  { name: '클라이언트', href: '/clients', icon: KeyIcon },
  { name: '역할', href: '/roles', icon: ShieldCheckIcon },
  { name: '세션', href: '/sessions', icon: ClockIcon },
  { name: '감사 로그', href: '/audit', icon: DocumentTextIcon },
  { name: '설정', href: '/settings', icon: Cog6ToothIcon },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <div className="flex flex-col w-64 bg-gray-900 min-h-screen">
      <div className="flex items-center h-16 px-6">
        <h1 className="text-white text-lg font-bold">AuthFusion</h1>
        <span className="ml-2 text-xs text-gray-400 bg-gray-800 px-2 py-0.5 rounded">Admin</span>
      </div>
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navigation.map((item) => {
          const isActive = pathname === item.href ||
            (item.href !== '/' && pathname.startsWith(item.href));
          return (
            <Link
              key={item.name}
              href={item.href}
              className={clsx(
                'flex items-center px-3 py-2.5 text-sm font-medium rounded-lg transition-colors',
                isActive
                  ? 'bg-primary-600 text-white'
                  : 'text-gray-300 hover:bg-gray-800 hover:text-white'
              )}
            >
              <item.icon className="w-5 h-5 mr-3 flex-shrink-0" />
              {item.name}
            </Link>
          );
        })}
      </nav>
      <div className="px-4 py-4 border-t border-gray-800">
        <p className="text-xs text-gray-500">AuthFusion SSO v1.0.0</p>
      </div>
    </div>
  );
}
