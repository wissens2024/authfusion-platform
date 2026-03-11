'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api';
import { setAccessToken } from '@/lib/auth';

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [mfaCode, setMfaCode] = useState('');
  const [mfaToken, setMfaToken] = useState('');
  const [showMfa, setShowMfa] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await authApi.login({ username, password });
      if (data.mfaRequired && data.mfaToken) {
        setMfaToken(data.mfaToken);
        setShowMfa(true);
      } else {
        setAccessToken(data.accessToken);
        router.push('/');
      }
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleMfaVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await authApi.mfaVerify({ mfaToken, code: mfaCode });
      setAccessToken(data.accessToken);
      router.push('/');
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'MFA 인증에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-gray-900">AuthFusion</h1>
          <p className="text-sm text-gray-500 mt-1">Admin Console</p>
        </div>

        {error && (
          <div className="bg-red-50 text-red-700 px-4 py-3 rounded-lg text-sm mb-4">{error}</div>
        )}

        {!showMfa ? (
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">사용자명</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="input-field"
                required
                autoFocus
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="input-field"
                required
              />
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full">
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleMfaVerify} className="space-y-4">
            <p className="text-sm text-gray-600">인증 앱의 6자리 코드를 입력하세요.</p>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">MFA 코드</label>
              <input
                type="text"
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value)}
                className="input-field text-center text-lg tracking-widest"
                maxLength={6}
                pattern="[0-9]{6}"
                required
                autoFocus
              />
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full">
              {loading ? '확인 중...' : '인증 확인'}
            </button>
            <button
              type="button"
              onClick={() => { setShowMfa(false); setError(''); }}
              className="btn-secondary w-full"
            >
              뒤로
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
