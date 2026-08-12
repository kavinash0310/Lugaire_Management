"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Settlement = {
  id: number;
  settlementId: string;
  platform: string;
  settlementDate: string;
  settlementPeriodStart: string;
  settlementPeriodEnd: string;
  grossAmount: number;
  marketplaceFees: number;
  shippingCharges: number;
  returnCharges: number;
  otherCharges: number;
  netAmount: number;
  receivedAmount: number;
  difference: number;
  status: string;
  reconciliationStatus: string;
  remarks?: string;
};

type Page = { content: Settlement[]; page: number; totalPages: number; totalElements: number };

const toCurrency = (value: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(value || 0);

export default function SettlementsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [platform, setPlatform] = useState("");
  const [status, setStatus] = useState("");
  const [reconciliationStatus, setReconciliationStatus] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [data, setData] = useState<Page>({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const query = useMemo(() => {
    const params = new URLSearchParams({ search, page: String(page), size: "20" });
    if (platform) params.set("platform", platform);
    if (status) params.set("status", status);
    if (reconciliationStatus) params.set("reconciliationStatus", reconciliationStatus);
    if (fromDate) params.set("fromDate", fromDate);
    if (toDate) params.set("toDate", toDate);
    return params.toString();
  }, [search, page, platform, status, reconciliationStatus, fromDate, toDate]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<Page>(`/settlements?${query}`);
      setData(response.data);
    } catch {
      setError("Unable to load settlements.");
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    void load();
  }, [load]);

  const reconcile = async (id: number) => {
    try {
      await api.patch(`/settlements/${id}/reconcile`);
      await load();
    } catch {
      setError("Unable to reconcile this settlement.");
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Settlements</h1>
          </div>
          <Link href="/settlements/new" className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white">Create settlement</Link>
        </div>

        <div className="mt-6 grid gap-3 rounded-lg border bg-card p-4 md:grid-cols-3 xl:grid-cols-6">
          <input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} placeholder="Search settlement ID" className="rounded border px-3 py-2" />
          <select value={platform} onChange={(event) => { setPlatform(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All platforms</option>
            {["MEESHO", "AMAZON", "FLIPKART", "WEBSITE", "OTHER"].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
          <select value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All statuses</option>
            {["PENDING", "PARTIALLY_RECEIVED", "RECEIVED", "RECONCILED"].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
          <select value={reconciliationStatus} onChange={(event) => { setReconciliationStatus(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All reconciliation</option>
            {["UNMATCHED", "MATCHED", "MISMATCH"].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
          <input type="date" value={fromDate} onChange={(event) => { setFromDate(event.target.value); setPage(0); }} className="rounded border px-3 py-2" />
          <input type="date" value={toDate} onChange={(event) => { setToDate(event.target.value); setPage(0); }} className="rounded border px-3 py-2" />
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {loading ? <p className="mt-6 text-sm text-muted-foreground">Loading settlements...</p> : data.content.length === 0 ? <p className="mt-6 rounded border bg-card p-6 text-sm text-muted-foreground">No settlements found.</p> : (
          <div className="mt-6 overflow-x-auto rounded-lg border bg-card shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Settlement ID</th>
                  <th className="px-4 py-3">Platform</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Period</th>
                  <th className="px-4 py-3">Gross</th>
                  <th className="px-4 py-3">Fees</th>
                  <th className="px-4 py-3">Net</th>
                  <th className="px-4 py-3">Received</th>
                  <th className="px-4 py-3">Difference</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Reconciliation</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((settlement) => (
                  <tr key={settlement.id} className="border-t">
                    <td className="px-4 py-3 font-medium"><Link href={`/settlements/${settlement.id}`} className="underline">{settlement.settlementId}</Link></td>
                    <td className="px-4 py-3">{settlement.platform}</td>
                    <td className="px-4 py-3">{settlement.settlementDate}</td>
                    <td className="px-4 py-3">{settlement.settlementPeriodStart} → {settlement.settlementPeriodEnd}</td>
                    <td className="px-4 py-3">{toCurrency(settlement.grossAmount)}</td>
                    <td className="px-4 py-3">{toCurrency(settlement.marketplaceFees)}</td>
                    <td className="px-4 py-3">{toCurrency(settlement.netAmount)}</td>
                    <td className="px-4 py-3">{toCurrency(settlement.receivedAmount)}</td>
                    <td className="px-4 py-3">{toCurrency(settlement.difference)}</td>
                    <td className="px-4 py-3">{settlement.status}</td>
                    <td className="px-4 py-3">{settlement.reconciliationStatus}</td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-3">
                        <Link href={`/settlements/${settlement.id}/edit`} className="text-slate-700 hover:underline">Edit</Link>
                        <button onClick={() => void reconcile(settlement.id)} className="text-slate-700 hover:underline">Reconcile</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="mt-4 flex items-center gap-3">
          <button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded border px-3 py-2 disabled:opacity-50">Previous</button>
          <span className="text-sm">Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
          <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((current) => current + 1)} className="rounded border px-3 py-2 disabled:opacity-50">Next</button>
        </div>
      </section>
    </AppShell>
  );
}
