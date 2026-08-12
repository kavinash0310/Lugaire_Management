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
