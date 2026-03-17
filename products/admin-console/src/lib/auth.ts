'use client';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const TOKEN_EXPIRY_KEY = 'tokenExpiresAt';
const TOKEN_BUFFER_MS = 30_000; // 만료 30초 전에 만료 처리

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (!token) return null;

  // 만료 검증 (FIA_UAU.1)
  if (isTokenExpired(token)) {
    return null; // 만료 시 clearAuth 하지 않음 - refresh 시도 가능
  }
  return token;
}

export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setAuth(accessToken: string, expiresIn: number, refreshToken?: string): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  const expiresAt = Date.now() + expiresIn * 1000;
  localStorage.setItem(TOKEN_EXPIRY_KEY, String(expiresAt));
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function clearAuth(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
}

/**
 * JWT exp 클레임과 로컬 만료 시각 모두 검증 (FIA_UAU.1)
 */
function isTokenExpired(token: string): boolean {
  // 1차: 로컬 저장된 만료 시각 확인
  const storedExpiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
  if (storedExpiry) {
    const expiresAt = Number(storedExpiry);
    if (Date.now() >= expiresAt - TOKEN_BUFFER_MS) return true;
  }

  // 2차: JWT exp 클레임 직접 확인
  const claims = parseJwt(token);
  if (!claims || !claims.exp) return true;
  const expMs = (claims.exp as number) * 1000;
  return Date.now() >= expMs - TOKEN_BUFFER_MS;
}

export function isAuthenticated(): boolean {
  // access token 유효하거나 refresh token이 있으면 인증 상태
  return !!getAccessToken() || !!getRefreshToken();
}

/**
 * ADMIN 역할 검증 (FDP_ACC.1 - 접근 제어)
 */
export function isAdmin(): boolean {
  const user = getCurrentUser();
  if (!user) return false;
  return user.roles.includes('ADMIN');
}

export function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export function getCurrentUser(): {
  sub: string;
  username: string;
  roles: string[];
  email?: string;
} | null {
  // access token 또는 refresh token에서 사용자 정보 추출
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  const token = accessToken || refreshToken;
  if (!token) return null;
  const claims = parseJwt(token);
  if (!claims) return null;
  return {
    sub: claims.sub as string,
    username: (claims.preferred_username as string) || (claims.sub as string),
    roles: (claims.roles as string[]) || [],
    email: claims.email as string | undefined,
  };
}

/**
 * 토큰 남은 시간(ms) 반환. 세션 타이머용.
 */
export function getTokenRemainingMs(): number {
  const storedExpiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
  if (!storedExpiry) return 0;
  return Math.max(0, Number(storedExpiry) - Date.now());
}
