import axios from 'axios';
import type {
  User, UserCreateRequest, Client, ClientCreateRequest,
  Role, RoleCreateRequest, Session, AuditEvent, AuditStatistics,
  HealthStatus, LoginRequest, LoginResponse, MfaVerifyRequest,
} from './types';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_SSO_SERVER_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth
export const authApi = {
  login: (data: LoginRequest) => api.post<LoginResponse>('/api/v1/auth/login', data),
  mfaVerify: (data: MfaVerifyRequest) =>
    api.post<LoginResponse>(`/api/v1/auth/mfa/verify?mfaToken=${encodeURIComponent(data.mfaToken)}`, { code: data.code }),
  logout: () => api.post('/api/v1/auth/logout'),
};

// Users
export const userApi = {
  list: (params?: { page?: number; size?: number; search?: string }) =>
    api.get<User[]>('/api/v1/users', { params }),
  get: (id: number) => api.get<User>(`/api/v1/users/${id}`),
  create: (data: UserCreateRequest) => api.post<User>('/api/v1/users', data),
  update: (id: number, data: Partial<UserCreateRequest>) => api.put<User>(`/api/v1/users/${id}`, data),
  delete: (id: number) => api.delete(`/api/v1/users/${id}`),
};

// Clients
export const clientApi = {
  list: () => api.get<Client[]>('/api/v1/clients'),
  get: (id: number) => api.get<Client>(`/api/v1/clients/${id}`),
  create: (data: ClientCreateRequest) => api.post<Client>('/api/v1/clients', data),
  update: (id: number, data: Partial<ClientCreateRequest>) => api.put<Client>(`/api/v1/clients/${id}`, data),
  delete: (id: number) => api.delete(`/api/v1/clients/${id}`),
};

// Roles
export const roleApi = {
  list: () => api.get<Role[]>('/api/v1/roles'),
  create: (data: RoleCreateRequest) => api.post<Role>('/api/v1/roles', data),
  delete: (id: number) => api.delete(`/api/v1/roles/${id}`),
  assignToUser: (roleId: number, userId: number) =>
    api.post(`/api/v1/roles/${roleId}/users/${userId}`),
  removeFromUser: (roleId: number, userId: number) =>
    api.delete(`/api/v1/roles/${roleId}/users/${userId}`),
};

// Sessions
export const sessionApi = {
  list: () => api.get<Session[]>('/api/v1/sessions'),
  revoke: (sessionId: string) => api.delete(`/api/v1/sessions/${sessionId}`),
};

// Audit
export const auditApi = {
  list: (params?: { eventType?: string; username?: string; from?: string; to?: string; page?: number; size?: number }) =>
    api.get<AuditEvent[]>('/api/v1/audit/events', { params }),
  statistics: (params?: { from?: string; to?: string }) =>
    api.get<AuditStatistics>('/api/v1/audit/statistics', { params }),
};

// Health
export const healthApi = {
  check: () => api.get<HealthStatus>('/actuator/health'),
};

export default api;
