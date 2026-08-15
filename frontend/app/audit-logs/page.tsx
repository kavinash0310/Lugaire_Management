"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type AuditLog = {
  id: number;
  userId?: number | null;
  username?: string | null;
  action: string;
  module: string;
  entityType: string;
  entityId: string;
  description: string;
  oldValue?: string | null;
  newValue?: string | null;
  createdAt: string;
};

type Page = { content: AuditLog[]; page: number; size: number; totalElements: number; totalPages: number };

const modules = ["", "PRODUCT", "SKU", "INVENTORY", "ORDER", "RETURN", "SUPPLIER", "PURCHASE", "SETTLEMENT", "EXPENSE", "USER", "AUTH"];
const actions = ["", "CREATE", "UPDATE", "DELETE", "ACTIVATE", "DEACTIVATE", "STOCK_IN", "STOCK_OUT", "ADJUSTMENT", "STATUS_CHANGE", "LOGIN", "LOGOUT"];

export default function AuditLogsPage() {
  const [page, setPage] = useState(0);
  const [module, setModule] = useState("");
  const [action, setAction] = useState("");
  const [user, setUser] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [data, setData] = useState<Page>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const query = useMemo(() => {
    const params = new URLSearchParams({ page: String(page), size: "20" });
    if (module) params.set("module", module);
    if (action) params.set("action", action);
    if (user) params.set("user", user);
    if (fromDate) params.set("fromDate", fromDate);
    if (toDate) params.set("toDate", toDate);
    return params.toString();
  }, [page, module, action, user, fromDate, toDate]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<Page>(`/audit-logs?${query}`);
      setData(response.data);
    } catch {
      setError("Unable to load audit logs.");
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div>
          <p className="text-sm font-medium text-muted-foreground">History</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">Audit Logs</h1>
          <p className="mt-2 text-sm text-muted-foreground">Read-only history of meaningful business changes.</p>
        </div>

        <div className="mt-6 grid gap-3 rounded-lg border bg-card p-4 md:grid-cols-3 xl:grid-cols-5">
          <select value={module} onChange={(event) => { setModule(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            {modules.map((item) => <option key={item || "all-module"} value={item}>{item ? item : "All modules"}</option>)}
          </select>
          <select value={action} onChange={(event) => { setAction(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            {actions.map((item) => <option key={item || "all-action"} value={item}>{item ? item : "All actions"}</option>)}
          </select>
          <input value={user} onChange={(event) => { setUser(event.target.value); setPage(0); }} placeholder="User" className="rounded border px-3 py-2" />
          <input type="date" value={fromDate} onChange={(event) => { setFromDate(event.target.value); setPage(0); }} className="rounded border px-3 py-2" />
          <input type="date" value={toDate} onChange={(event) => { setToDate(event.target.value); setPage(0); }} className="rounded border px-3 py-2" />
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {loading ? (
          <p className="mt-6 text-sm text-muted-foreground">Loading audit logs...</p>
        ) : data.content.length === 0 ? (
          <p className="mt-6 rounded border bg-card p-6 text-sm text-muted-foreground">No audit logs found.</p>
        ) : (
          <div className="mt-6 overflow-x-auto rounded-lg border bg-card shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Date / Time</th>
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Module</th>
                  <th className="px-4 py-3">Entity</th>
                  <th className="px-4 py-3">Entity ID</th>
                  <th className="px-4 py-3">Description</th>
                  <th className="px-4 py-3 text-right">Details</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((entry) => (
                  <tr key={entry.id} className="border-t">
                    <td className="px-4 py-3 whitespace-nowrap">{new Date(entry.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-3">{entry.username ?? `#${entry.userId ?? "-"}`}</td>
                    <td className="px-4 py-3">{entry.action}</td>
                    <td className="px-4 py-3">{entry.module}</td>
                    <td className="px-4 py-3">{entry.entityType}</td>
                    <td className="px-4 py-3">{entry.entityId}</td>
                    <td className="px-4 py-3">{entry.description}</td>
                    <td className="px-4 py-3 text-right">
                      <Link href={`/audit-logs/${entry.id}`} className="text-slate-700 hover:underline">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="mt-4 flex items-center gap-3">
          <button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded border px-3 py-2 disabled:opacity-50">
            Previous
          </button>
          <span className="text-sm">Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
          <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((current) => current + 1)} className="rounded border px-3 py-2 disabled:opacity-50">
            Next
          </button>
        </div>
      </section>
    </AppShell>
  );
}
