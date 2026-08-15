"use client";

import { AppShell } from "@/components/app-shell";
import { fetchRoles, type Role, type User } from "@/lib/auth";
import { api } from "@/lib/api";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { useAuth } from "@/components/auth-provider";

type UserPage = {
  content: User[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type UserFormValues = {
  name: string;
  email: string;
  password: string;
  roleCode: string;
  active: boolean;
};

const emptyForm: UserFormValues = {
  name: "",
  email: "",
  password: "",
  roleCode: "ADMIN",
  active: true,
};

export default function UsersPage() {
  const { user } = useAuth();
  const [roles, setRoles] = useState<Role[]>([]);
  const [users, setUsers] = useState<UserPage>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [role, setRole] = useState("");
  const [active, setActive] = useState("");
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);
  const [form, setForm] = useState<UserFormValues>(emptyForm);

  const isAdmin = user?.roleCode === "ADMIN";

  const loadRoles = async () => {
    try {
      const response = await fetchRoles();
      setRoles(response);
      if (response.length > 0 && !form.roleCode) {
        setForm((current) => ({ ...current, roleCode: response[0].code }));
      }
    } catch {
      setError("Unable to load roles.");
    }
  };

  const loadUsers = async () => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams();
      if (query.trim()) params.set("q", query.trim());
      if (role) params.set("role", role);
      if (active) params.set("active", active);
      params.set("page", String(page));
      params.set("size", "10");
      const response = await api.get<UserPage>(`/users?${params.toString()}`);
      setUsers(response.data);
    } catch {
      setError("Unable to load users. Check backend auth and your session.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadRoles();
  }, []);

  useEffect(() => {
    if (isAdmin) {
      void loadUsers();
    }
  }, [active, isAdmin, page, query, role]);

  const openCreate = () => {
    setEditing(null);
    setForm({ ...emptyForm, roleCode: roles[0]?.code ?? "ADMIN" });
    setShowForm(true);
  };

  const openEdit = (account: User) => {
    setEditing(account);
    setForm({
      name: account.name,
      email: account.email,
      password: "",
      roleCode: account.roleCode,
      active: account.active,
    });
    setShowForm(true);
  };

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim(),
        roleCode: form.roleCode,
        active: form.active,
      };
      if (editing) {
        await api.put(`/users/${editing.id}`, payload);
      } else {
        if (!form.password.trim()) {
          setError("Password is required for new users.");
          return;
        }
        await api.post("/users", { ...payload, password: form.password });
      }
      setShowForm(false);
      setForm(emptyForm);
      await loadUsers();
    } catch {
      setError("Unable to save the user. Check validation, email uniqueness, and role selection.");
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (account: User) => {
    const nextState = !account.active;
    if (!window.confirm(`${nextState ? "Activate" : "Deactivate"} ${account.name}?`)) {
      return;
    }
    try {
      await api.patch(`/users/${account.id}/active`, { active: nextState });
      await loadUsers();
    } catch {
      setError("Unable to update the active status.");
    }
  };

  const roleLabelByCode = useMemo(() => new Map(roles.map((item) => [item.code, item.name])), [roles]);

  if (!isAdmin) {
    return (
      <AppShell>
        <section className="mx-auto max-w-4xl p-5 md:p-8">
          <div className="rounded-xl border bg-card p-6">
            <p className="text-sm text-muted-foreground">Access control</p>
            <h1 className="mt-1 text-2xl font-semibold">Users</h1>
            <p className="mt-3 text-sm text-muted-foreground">This screen is available to administrators only.</p>
          </div>
        </section>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Admin</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Users</h1>
          </div>
          <button onClick={openCreate} className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800">
            Add User
          </button>
        </div>

        <div className="mt-6 grid gap-3 rounded-xl border bg-card p-4 md:grid-cols-4">
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search users..." className="rounded-lg border px-3 py-2 text-sm" />
          <select value={role} onChange={(event) => setRole(event.target.value)} className="rounded-lg border px-3 py-2 text-sm">
            <option value="">All roles</option>
            {roles.map((item) => (
              <option key={item.id} value={item.code}>
                {item.name}
              </option>
            ))}
          </select>
          <select value={active} onChange={(event) => setActive(event.target.value)} className="rounded-lg border px-3 py-2 text-sm">
            <option value="">All status</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
          <button
            type="button"
            onClick={() => {
              setQuery("");
              setRole("");
              setActive("");
              setPage(0);
            }}
            className="rounded-lg border px-3 py-2 text-sm font-medium hover:bg-muted"
          >
            Reset
          </button>
        </div>

        <div className="mt-6 overflow-hidden rounded-xl border bg-card shadow-sm">
          {error ? (
            <p className="p-5 text-sm text-red-700">{error}</p>
          ) : loading ? (
            <p className="p-5 text-sm text-muted-foreground">Loading users...</p>
          ) : users.content.length === 0 ? (
            <p className="p-5 text-sm text-muted-foreground">No users found. Create the first staff account.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted text-muted-foreground">
                  <tr>
                    <th className="p-4">Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.content.map((account) => (
                    <tr key={account.id} className="border-t">
                      <td className="p-4 font-medium">{account.name}</td>
                      <td>{account.email}</td>
                      <td>{roleLabelByCode.get(account.roleCode) ?? account.roleName}</td>
                      <td>{account.active ? "Active" : "Inactive"}</td>
                      <td>{new Date(account.createdAt).toLocaleDateString()}</td>
                      <td className="p-4 text-right">
                        <div className="flex justify-end gap-2">
                          <button onClick={() => openEdit(account)} className="rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-muted">
                            Edit
                          </button>
                          <button onClick={() => void toggleActive(account)} className="rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-muted">
                            {account.active ? "Deactivate" : "Activate"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {users.totalPages > 1 ? (
          <div className="mt-4 flex items-center justify-between text-sm">
            <p className="text-muted-foreground">
              Page {users.page + 1} of {users.totalPages}
            </p>
            <div className="flex gap-2">
              <button disabled={users.page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))} className="rounded-md border px-3 py-2 disabled:opacity-40">
                Previous
              </button>
              <button disabled={users.page + 1 >= users.totalPages} onClick={() => setPage((current) => current + 1)} className="rounded-md border px-3 py-2 disabled:opacity-40">
                Next
              </button>
            </div>
          </div>
        ) : null}
      </section>

      {showForm ? (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/40 p-4">
          <form onSubmit={save} className="w-full max-w-xl rounded-2xl bg-card p-6 shadow-2xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm text-muted-foreground">{editing ? "Edit user" : "Add user"}</p>
                <h2 className="text-xl font-semibold">{editing ? "Update account" : "Create account"}</h2>
              </div>
              <button type="button" onClick={() => setShowForm(false)} className="rounded-md border px-3 py-1.5 text-sm">
                Close
              </button>
            </div>
            <div className="mt-6 grid gap-4 md:grid-cols-2">
              <label className="block text-sm font-medium">
                Name
                <input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
              </label>
              <label className="block text-sm font-medium">
                Email
                <input
                  type="email"
                  value={form.email}
                  onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                  className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
                />
              </label>
              <label className="block text-sm font-medium">
                Role
                <select value={form.roleCode} onChange={(event) => setForm((current) => ({ ...current, roleCode: event.target.value }))} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm">
                  {roles.map((item) => (
                    <option key={item.id} value={item.code}>
                      {item.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm font-medium">
                Status
                <select
                  value={String(form.active)}
                  onChange={(event) => setForm((current) => ({ ...current, active: event.target.value === "true" }))}
                  className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
                >
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
              </label>
              {!editing ? (
                <label className="block text-sm font-medium md:col-span-2">
                  Password
                  <input
                    type="password"
                    value={form.password}
                    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                    className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
                  />
                </label>
              ) : null}
            </div>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button type="button" onClick={() => setShowForm(false)} className="rounded-md border px-4 py-2 text-sm font-medium">
                Cancel
              </button>
              <button disabled={saving} className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50">
                {saving ? "Saving..." : "Save"}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </AppShell>
  );
}
