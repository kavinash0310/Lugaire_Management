"use client";

import Link from "next/link";
import { use, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Item = {
  id: number;
  orderId: number;
  orderNumber: string;
  orderDate: string;
  grossOrderAmount: number;
  marketplaceFee: number;
  shippingCharge: number;
  returnCharge: number;
  otherCharge: number;
  expectedNetSettlement: number;
  settledAmount: number;
  difference: number;
  reconciliationStatus: string;
  remarks?: string;
};

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
  items: Item[];
};

const toCurrency = (value: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(value || 0);

export default function SettlementDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [settlement, setSettlement] = useState<Settlement | null>(null);
  const [status, setStatus] = useState("PENDING");
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<Settlement>(`/settlements/${id}`).then((response) => {
      setSettlement(response.data);
      setStatus(response.data.status);
    }).catch(() => setError("Unable to load settlement."));
  }, [id]);

  const reconcile = async () => {
    if (!settlement) return;
    try {
      const response = await api.patch<Settlement>(`/settlements/${settlement.id}/reconcile`);
      setSettlement(response.data);
      setStatus(response.data.status);
    } catch {
      setError("Unable to reconcile settlement.");
    }
  };

  const updateStatus = async () => {
    if (!settlement) return;
    try {
      const response = await api.patch<Settlement>(`/settlements/${settlement.id}/status`, { status });
      setSettlement(response.data);
      setStatus(response.data.status);
    } catch {
      setError("Unable to update settlement status.");
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold">{settlement?.settlementId ?? "Settlement"}</h1>
          </div>
          <div className="flex gap-3">
            <Link href="/settlements" className="text-sm underline">Back</Link>
            <Link href={`/settlements/${id}/edit`} className="text-sm underline">Edit</Link>
          </div>
        </div>

        {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {!settlement ? <p className="mt-6 text-sm text-muted-foreground">Loading settlement...</p> : (
          <div className="mt-6 space-y-6">
            <div className="grid gap-4 md:grid-cols-3">
              <Card label="Platform" value={settlement.platform} />
              <Card label="Date" value={settlement.settlementDate} />
              <Card label="Period" value={`${settlement.settlementPeriodStart} → ${settlement.settlementPeriodEnd}`} />
              <Card label="Status" value={settlement.status} />
              <Card label="Reconciliation" value={settlement.reconciliationStatus} />
              <Card label="Difference" value={toCurrency(settlement.difference)} />
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              <Card label="Gross" value={toCurrency(settlement.grossAmount)} />
              <Card label="Fees" value={toCurrency(settlement.marketplaceFees)} />
              <Card label="Shipping" value={toCurrency(settlement.shippingCharges)} />
              <Card label="Net" value={toCurrency(settlement.netAmount)} />
              <Card label="Received" value={toCurrency(settlement.receivedAmount)} />
              <Card label="Returns" value={toCurrency(settlement.returnCharges)} />
              <Card label="Other" value={toCurrency(settlement.otherCharges)} />
            </div>

            <div className="rounded-lg border bg-card p-5 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold">Status actions</h2>
                  <p className="text-sm text-muted-foreground">Reconcile to update payment confirmation based on actual settlement amounts.</p>
                </div>
                <div className="flex items-center gap-2">
                  <select value={status} onChange={(event) => setStatus(event.target.value)} className="rounded border bg-background px-3 py-2 text-sm">
                    {["PENDING", "PARTIALLY_RECEIVED", "RECEIVED", "RECONCILED"].map((value) => <option key={value} value={value}>{value}</option>)}
                  </select>
                  <button onClick={() => void updateStatus()} className="rounded border px-3 py-2 text-sm">Update status</button>
                  <button onClick={() => void reconcile()} className="rounded bg-slate-900 px-3 py-2 text-sm text-white">Reconcile</button>
                </div>
              </div>
            </div>

            <div className="rounded-lg border bg-card shadow-sm">
              <div className="border-b px-5 py-4">
                <h2 className="text-lg font-semibold">Settlement items</h2>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-muted text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3">Order</th>
                      <th className="px-4 py-3">Date</th>
                      <th className="px-4 py-3">Gross</th>
                      <th className="px-4 py-3">Fees</th>
                      <th className="px-4 py-3">Deductions</th>
                      <th className="px-4 py-3">Expected</th>
                      <th className="px-4 py-3">Actual</th>
                      <th className="px-4 py-3">Difference</th>
                      <th className="px-4 py-3">Reconciliation</th>
                    </tr>
                  </thead>
                  <tbody>
                    {settlement.items.map((item) => (
                      <tr key={item.id} className="border-t">
                        <td className="px-4 py-3 font-medium"><Link className="underline" href={`/orders/${item.orderId}`}>{item.orderNumber}</Link></td>
                        <td className="px-4 py-3">{item.orderDate}</td>
                        <td className="px-4 py-3">{toCurrency(item.grossOrderAmount)}</td>
                        <td className="px-4 py-3">{toCurrency(item.marketplaceFee)}</td>
                        <td className="px-4 py-3">{toCurrency(item.shippingCharge + item.returnCharge + item.otherCharge)}</td>
                        <td className="px-4 py-3">{toCurrency(item.expectedNetSettlement)}</td>
                        <td className="px-4 py-3">{toCurrency(item.settledAmount)}</td>
                        <td className="px-4 py-3">{toCurrency(item.difference)}</td>
                        <td className="px-4 py-3">{item.reconciliationStatus}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </section>
    </AppShell>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-card p-4 shadow-sm">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 text-lg font-semibold">{value}</p>
    </div>
  );
}
