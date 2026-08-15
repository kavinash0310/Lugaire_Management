"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
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

const pretty = (value?: string | null) => {
  if (!value) {
    return "-";
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

export default function AuditLogDetailPage() {
  const params = useParams<{ id: string }>();
  const [entry, setEntry] = useState<AuditLog | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await api.get<AuditLog>(`/audit-logs/${params.id}`);
        setEntry(response.data);
      } catch {
        setError("Unable to load the audit entry.");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [params.id]);

  return (
    <AppShell>
      <section className="mx-auto max-w-5xl p-5 md:p-8">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Audit entry</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Audit Log Detail</h1>
          </div>
          <Link href="/audit-logs" className="rounded-md border px-4 py-2 text-sm font-medium">Back</Link>
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {loading ? (
          <p className="mt-6 text-sm text-muted-foreground">Loading audit entry...</p>
        ) : entry ? (
          <div className="mt-6 rounded-xl border bg-card p-5 shadow-sm">
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Date / Time" value={new Date(entry.createdAt).toLocaleString()} />
              <Field label="User" value={entry.username ?? `#${entry.userId ?? "-"}`} />
              <Field label="Action" value={entry.action} />
              <Field label="Module" value={entry.module} />
              <Field label="Entity" value={entry.entityType} />
              <Field label="Entity ID" value={entry.entityId} />
            </div>
            <div className="mt-5 rounded-lg bg-muted p-4">
              <p className="text-sm font-medium text-muted-foreground">Description</p>
              <p className="mt-1 text-sm">{entry.description}</p>
            </div>
            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Before</p>
                <pre className="mt-2 overflow-x-auto rounded-lg border bg-slate-950 p-4 text-xs text-slate-100">{pretty(entry.oldValue)}</pre>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">After</p>
                <pre className="mt-2 overflow-x-auto rounded-lg border bg-slate-950 p-4 text-xs text-slate-100">{pretty(entry.newValue)}</pre>
              </div>
            </div>
          </div>
        ) : null}
      </section>
    </AppShell>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-muted/30 p-4">
      <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm">{value}</p>
    </div>
  );
}
