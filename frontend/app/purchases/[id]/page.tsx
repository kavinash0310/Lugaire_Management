"use client";

import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

type PurchaseItem = {
  id: number;
  variantId: number;
  productName: string;
  colorName: string;
  sizeName: string;
  skuSnapshot: string;
  quantity: number;
  unitCost: number | string;
  tax: number | string;
  lineTotal: number | string;
};

type Purchase = {
  id: number;
  purchaseId: string;
  supplierId: number;
  supplierName: string;
  supplierCode: string;
  purchaseDate: string;
  invoiceNumber: string;
  invoiceDate: string;
  subtotal: number | string;
  tax: number | string;
  otherCharges: number | string;
  totalAmount: number | string;
  paymentStatus: string;
  paidAmount: number | string;
  dueAmount: number | string;
  purchaseStatus: string;
  inventoryReceived: boolean;
  remarks?: string | null;
  items: PurchaseItem[];
};

const paymentStatuses = ["PENDING", "PARTIALLY_PAID", "PAID"];
const purchaseStatuses = ["DRAFT", "RECEIVED", "CANCELLED"];

export default function PurchaseDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [purchase, setPurchase] = useState<Purchase | null>(null);
  const [paymentStatus, setPaymentStatus] = useState("PENDING");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<Purchase>(`/purchases/${id}`);
      setPurchase(response.data);
      setPaymentStatus(response.data.paymentStatus);
    } catch {
      setError("Unable to load purchase details.");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const updateStatus = async (status: string) => {
    if (!window.confirm(`Mark this purchase as ${status}?`)) return;
    setSaving(true);
    try {
      await api.patch(`/purchases/${id}/status`, { status });
      await load();
    } catch {
      setError("Unable to update purchase status.");
    } finally {
      setSaving(false);
    }
  };

  const updatePaymentStatus = async () => {
    setSaving(true);
    try {
      await api.patch(`/purchases/${id}/payment-status`, { status: paymentStatus });
      await load();
    } catch {
      setError("Unable to update payment status.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Procurement</p>
            <h1 className="mt-1 text-2xl font-semibold">Purchase details</h1>
          </div>
          <div className="flex items-center gap-3">
            <Link href={`/purchases/${id}/edit`} className="text-sm underline">
              Edit
            </Link>
            <Link href="/purchases" className="text-sm underline">
              Back to purchases
            </Link>
          </div>
        </div>

        {loading && <p className="mt-6">Loading purchase...</p>}
        {error && <p className="mt-6 text-destructive">{error}</p>}

        {purchase && (
          <div className="mt-6 space-y-6">
            <div className="grid gap-4 md:grid-cols-3">
              <Card label="Supplier" value={`${purchase.supplierName} (${purchase.supplierCode})`} />
              <Card label="Purchase ID" value={purchase.purchaseId} />
              <Card label="Invoice" value={purchase.invoiceNumber} />
              <Card label="Purchase date" value={purchase.purchaseDate} />
              <Card label="Status" value={purchase.purchaseStatus} />
              <Card label="Payment" value={purchase.paymentStatus} />
              <Card label="Inventory received" value={purchase.inventoryReceived ? "Yes" : "No"} />
              <Card label="Total" value={`₹${Number(purchase.totalAmount).toFixed(2)}`} />
              <Card label="Due" value={`₹${Number(purchase.dueAmount).toFixed(2)}`} />
            </div>

            {purchase.remarks && <div className="rounded border bg-card p-4 text-sm"><p className="font-medium">Remarks</p><p className="mt-1 text-muted-foreground">{purchase.remarks}</p></div>}

            <div className="grid gap-4 rounded border bg-card p-4 md:grid-cols-2">
              <div>
                <h2 className="font-semibold">Purchase status</h2>
                <div className="mt-3 flex flex-wrap gap-2">
                  {purchaseStatuses.map((status) => (
                    <button key={status} disabled={saving || purchase.purchaseStatus === status} onClick={() => void updateStatus(status)} className="rounded border px-3 py-2 text-sm disabled:opacity-50">
                      {status}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <h2 className="font-semibold">Payment status</h2>
                <div className="mt-3 flex flex-wrap gap-3">
                  <select value={paymentStatus} onChange={(event) => setPaymentStatus(event.target.value)} className="rounded border bg-background px-3 py-2">
                    {paymentStatuses.map((status) => <option key={status} value={status}>{status}</option>)}
                  </select>
                  <button disabled={saving} onClick={() => void updatePaymentStatus()} className="rounded bg-slate-900 px-4 py-2 text-white disabled:opacity-50">
                    Save
                  </button>
                </div>
              </div>
            </div>

            <div className="overflow-x-auto rounded border bg-card">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted">
                  <tr>
                    <th className="p-3">Product</th>
                    <th>SKU</th>
                    <th>Qty</th>
                    <th>Unit cost</th>
                    <th>Tax</th>
                    <th>Line total</th>
                  </tr>
                </thead>
                <tbody>
                  {purchase.items.map((item) => (
                    <tr key={item.id} className="border-t">
                      <td className="p-3">{item.productName} / {item.colorName} / {item.sizeName}</td>
                      <td>{item.skuSnapshot}</td>
                      <td>{item.quantity}</td>
                      <td>₹{Number(item.unitCost).toFixed(2)}</td>
                      <td>₹{Number(item.tax).toFixed(2)}</td>
                      <td>₹{Number(item.lineTotal).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>
    </AppShell>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded border bg-card p-4">
      <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-medium">{value}</p>
    </div>
  );
}
