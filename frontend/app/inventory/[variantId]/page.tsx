"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Movement = {
  id: number;
  transactionDate: string;
  createdAt: string;
  sku: string;
  productName: string;
  movementType: string;
  quantity: number;
  previousStock: number | null;
  newStock: number | null;
  referenceType: string | null;
  referenceId: string | null;
  reason: string | null;
  remarks: string | null;
  userId: number | null;
  userName: string | null;
  unitCost: string | null;
};

type InventoryItem = {
  variantId: number;
  sku: string;
  productName: string;
  brandName: string;
  categoryName: string;
  color: string;
  size: string;
  currentStock: number;
  lowStockThreshold: number;
  status: "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK";
  unitCost: string;
  inventoryValue: string;
};

type DetailResponse = {
  summary: InventoryItem;
  movements: Movement[];
};

function formatCurrency(value: string | null) {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number.isFinite(amount) ? amount : 0);
}

function formatDate(value: string | null) {
  if (!value) return "-";
  return new Date(value).toLocaleDateString("en-IN", { year: "numeric", month: "short", day: "numeric" });
}

export default function InventoryDetailPage() {
  const params = useParams<{ variantId: string }>();
  const variantId = Number(params.variantId);
  const [detail, setDetail] = useState<DetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [adjustOpen, setAdjustOpen] = useState(false);
  const [form, setForm] = useState({
    adjustmentType: "ADD_STOCK",
    quantity: "1",
    reason: "",
    remarks: "",
    transactionDate: new Date().toISOString().slice(0, 10),
  });

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<DetailResponse>(`/inventory/${variantId}/detail`);
      setDetail(response.data);
    } catch {
      setError("Unable to load inventory detail. Confirm the variant exists and the backend is available.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (Number.isNaN(variantId)) {
      setError("Invalid variant id.");
      setLoading(false);
      return;
    }
    void load();
  }, [variantId]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!detail) return;
    setSaving(true);
    setError("");
    try {
      await api.post("/inventory/adjustments", {
        variantId: detail.summary.variantId,
        adjustmentType: form.adjustmentType,
        quantity: Number(form.quantity),
        reason: form.reason,
        remarks: form.remarks || null,
        transactionDate: form.transactionDate,
      });
      setAdjustOpen(false);
      await load();
    } catch {
      setError("Unable to save adjustment.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">
              <Link href="/inventory" className="hover:underline">
                Inventory
              </Link>{" "}
              / Detail
            </p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">{detail?.summary.productName ?? "Inventory detail"}</h1>
            <p className="mt-1 text-sm text-muted-foreground">SKU {detail?.summary.sku ?? "-"} - live stock history and adjustments</p>
          </div>
          <button onClick={() => setAdjustOpen(true)} className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white">
            Adjust stock
          </button>
        </div>

        {error && <p className="mt-6 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}

        {loading ? (
          <p className="mt-6 text-sm text-muted-foreground">Loading inventory detail...</p>
        ) : detail ? (
          <>
            <div className="mt-6 grid gap-4 md:grid-cols-4">
              <StatCard label="Current stock" value={String(detail.summary.currentStock)} />
              <StatCard label="Inventory value" value={formatCurrency(detail.summary.inventoryValue)} />
              <StatCard label="Reorder level" value={String(detail.summary.lowStockThreshold)} />
              <StatCard label="Status" value={detail.summary.status.replaceAll("_", " ")} />
            </div>

            <div className="mt-6 grid gap-4 md:grid-cols-2">
              <DetailCard label="Product" value={detail.summary.productName} />
              <DetailCard label="SKU" value={detail.summary.sku} />
              <DetailCard label="Brand" value={detail.summary.brandName} />
              <DetailCard label="Category" value={detail.summary.categoryName} />
              <DetailCard label="Variant" value={`${detail.summary.color} - ${detail.summary.size}`} />
              <DetailCard label="Unit cost" value={formatCurrency(detail.summary.unitCost)} />
            </div>

            <div className="mt-8 rounded-xl border bg-card shadow-sm">
              <div className="border-b p-4">
                <h2 className="text-lg font-semibold">Movement history</h2>
                <p className="text-sm text-muted-foreground">Latest transactions affecting this SKU variant.</p>
              </div>
              {detail.movements.length === 0 ? (
                <p className="p-6 text-sm text-muted-foreground">No inventory movements recorded yet.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-muted text-muted-foreground">
                      <tr>
                        <th className="px-5 py-3 font-medium">Date</th>
                        <th className="px-5 py-3 font-medium">Type</th>
                        <th className="px-5 py-3 font-medium">Qty</th>
                        <th className="px-5 py-3 font-medium">Stock</th>
                        <th className="px-5 py-3 font-medium">Reason / Remarks</th>
                        <th className="px-5 py-3 font-medium">User</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detail.movements.map((movement) => (
                        <tr key={movement.id} className="border-t">
                          <td className="px-5 py-3">
                            <div className="font-medium">{formatDate(movement.transactionDate)}</div>
                            <div className="text-xs text-muted-foreground">{formatDate(movement.createdAt)}</div>
                          </td>
                          <td className="px-5 py-3">{movement.movementType}</td>
                          <td className="px-5 py-3">{movement.quantity}</td>
                          <td className="px-5 py-3">
                            {movement.previousStock ?? "-"} {"->"} {movement.newStock ?? "-"}
                          </td>
                          <td className="px-5 py-3">
                            <div>{movement.reason ?? "-"}</div>
                            <div className="text-xs text-muted-foreground">{movement.remarks ?? movement.referenceType ?? "-"}</div>
                          </td>
                          <td className="px-5 py-3">
                            <div>{movement.userName ?? "SYSTEM"}</div>
                            <div className="text-xs text-muted-foreground">{movement.userId ?? "-"}</div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </>
        ) : null}
      </section>

      {adjustOpen && detail && (
        <div className="fixed inset-0 z-20 grid place-items-center bg-slate-950/40 p-4">
          <form onSubmit={submit} className="w-full max-w-2xl rounded-xl bg-card p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-lg font-semibold">Adjust stock</h2>
                <p className="text-sm text-muted-foreground">
                  {detail.summary.sku} - {detail.summary.productName}
                </p>
              </div>
              <button type="button" onClick={() => setAdjustOpen(false)} className="text-sm text-muted-foreground hover:text-foreground">
                Close
              </button>
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <label className="text-sm font-medium">
                Adjustment type
                <select
                  value={form.adjustmentType}
                  onChange={(event) => setForm({ ...form, adjustmentType: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                >
                  <option value="ADD_STOCK">Add stock</option>
                  <option value="REMOVE_STOCK">Remove stock</option>
                </select>
              </label>
              <label className="text-sm font-medium">
                Quantity
                <input
                  required
                  min="1"
                  type="number"
                  value={form.quantity}
                  onChange={(event) => setForm({ ...form, quantity: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                />
              </label>
              <label className="text-sm font-medium">
                Reason
                <input
                  required
                  value={form.reason}
                  onChange={(event) => setForm({ ...form, reason: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                  placeholder="Audit correction, damage, stock found, etc."
                />
              </label>
              <label className="text-sm font-medium">
                Transaction date
                <input
                  required
                  type="date"
                  value={form.transactionDate}
                  onChange={(event) => setForm({ ...form, transactionDate: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                />
              </label>
              <label className="text-sm font-medium md:col-span-2">
                Remarks
                <textarea
                  value={form.remarks}
                  onChange={(event) => setForm({ ...form, remarks: event.target.value })}
                  className="mt-1 min-h-24 w-full rounded-md border bg-background px-3 py-2"
                  placeholder="Optional context for the adjustment"
                />
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={() => setAdjustOpen(false)} className="rounded-md border px-4 py-2 text-sm font-medium">
                Cancel
              </button>
              <button disabled={saving} className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50">
                {saving ? "Saving..." : "Save adjustment"}
              </button>
            </div>
          </form>
        </div>
      )}
    </AppShell>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border bg-card p-4 shadow-sm">
      <p className="text-xs uppercase tracking-wider text-muted-foreground">{label}</p>
      <p className="mt-2 text-2xl font-semibold">{value}</p>
    </div>
  );
}

function DetailCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border bg-card p-4 shadow-sm">
      <p className="text-xs uppercase tracking-wider text-muted-foreground">{label}</p>
      <p className="mt-2 text-sm font-medium">{value}</p>
    </div>
  );
}
