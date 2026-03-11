export interface User {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED';
  roles: string[];
  mfaEnabled?: boolean;
  userSource?: 'LOCAL' | 'LDAP';
  createdAt: string;
  lastLoginAt?: string;
}

export interface UserCreateRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export interface Client {
  id: number;
  clientId: string;
  clientName: string;
  clientType: 'CONFIDENTIAL' | 'PUBLIC';
  redirectUris: string[];
  scopes: string[];
  grantTypes: string[];
  requirePkce: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface ClientCreateRequest {
  clientId: string;
  clientName: string;
  clientType: 'CONFIDENTIAL' | 'PUBLIC';
  redirectUris: string[];
  scopes: string[];
  grantTypes: string[];
  requirePkce: boolean;
}

export interface Role {
  id: number;
  name: string;
  description?: string;
  userCount?: number;
  createdAt: string;
}

export interface RoleCreateRequest {
  name: string;
  description?: string;
}

export interface Session {
  sessionId: string;
  username: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
  lastAccessedAt: string;
  expiresAt: string;
}

export interface AuditEvent {
  id: number;
  eventType: string;
  username?: string;
  clientId?: string;
  ipAddress?: string;
  success: boolean;
  details?: string;
  createdAt: string;
}

export interface AuditStatistics {
  totalEvents: number;
  successCount: number;
  failureCount: number;
  eventTypeCounts: Record<string, number>;
}

export interface HealthStatus {
  status: 'UP' | 'DOWN';
  components?: Record<string, { status: string; details?: Record<string, unknown> }>;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  sessionId?: string;
  user?: User;
  mfaRequired?: boolean;
  mfaToken?: string;
}

export interface MfaVerifyRequest {
  mfaToken: string;
  code: string;
}
