'use client';

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem('accessToken', token);
}

export function clearAccessToken(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('accessToken');
}

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

export function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export function getCurrentUser(): { sub: string; roles: string[]; email?: string } | null {
  const token = getAccessToken();
  if (!token) return null;
  const claims = parseJwt(token);
  if (!claims) return null;
  return {
    sub: claims.sub as string,
    roles: (claims.roles as string[]) || [],
    email: claims.email as string | undefined,
  };
}
