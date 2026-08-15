import { api } from "@/lib/api";

export type SessionUser = {
  id: number;
  name: string;
  email: string;
  roleCode: string;
  roleName: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Role = {
  id: number;
  code: string;
  name: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type User = SessionUser;

export async function login(request: LoginRequest) {
  const response = await api.post<SessionUser>("/auth/login", request);
  return response.data;
}

export async function fetchCurrentUser() {
  const response = await api.get<SessionUser>("/auth/me");
  return response.data;
}

export async function logout() {
  await api.post("/auth/logout");
}

export async function fetchRoles() {
  const response = await api.get<Role[]>("/roles");
  return response.data;
}

export type UserPage = {
  content: User[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type UserCreateRequest = {
  name: string;
  email: string;
  password: string;
  roleCode: string;
  active: boolean;
};

export type UserUpdateRequest = {
  name: string;
  email: string;
  roleCode: string;
  active: boolean;
};

export async function fetchUsers(params: { q?: string; role?: string; active?: boolean; page?: number; size?: number } = {}) {
  const query = new URLSearchParams();
  if (params.q) query.set("q", params.q);
  if (params.role) query.set("role", params.role);
  if (params.active !== undefined) query.set("active", String(params.active));
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));
  const response = await api.get<UserPage>(`/users?${query.toString()}`);
  return response.data;
}

export async function createUser(request: UserCreateRequest) {
  const response = await api.post<User>("/users", request);
  return response.data;
}

export async function updateUser(id: number, request: UserUpdateRequest) {
  const response = await api.put<User>(`/users/${id}`, request);
  return response.data;
}

export async function setUserActive(id: number, active: boolean) {
  const response = await api.patch<User>(`/users/${id}/active`, { active });
  return response.data;
}
