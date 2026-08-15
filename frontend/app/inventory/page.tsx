"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

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

type InventoryPageResponse = {
  content: InventoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type AdjustmentType = "ADD_STOCK" | "REMOVE_STOCK";

const statusFilters = [
  { label: "All stock", value: "" },
  { label: "Low stock", value: "LOW_STOCK" },
  { label: "Out of stock", value: "OUT_OF_STOCK" },
];

const sortOptions = [
  { label: "SKU", value: "SKU_ASC" },
  { label: "Stock: low to high", value: "STOCK_ASC" },
  { label: "Stock: high to low", value: "STOCK_DESC" },
  { label: "Product name", value: "PRODUCT_ASC" },
];

function formatCurrency(value: string) {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number.isFinite(amount) ? amount : 0);
}

export default function InventoryPage() {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [sortBy, setSortBy] = useState("SKU_ASC");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [adjusting, setAdjusting] = useState<InventoryItem | null>(null);
  const [saving, setSaving] = useState(false);
  const [adjustForm, setAdjustForm] = useState({
    adjustmentType: "ADD_STOCK" as AdjustmentType,
    quantity: "1",
    reason: "",
    remarks: "",
    transactionDate: new Date().toISOString().slice(0, 10),
  });

  const endpoint = useMemo(() => {
    const params = new URLSearchParams();
    if (query.trim()) params.set("query", query.trim());
    if (status) params.set("status", status);
    if (sortBy) params.set("sortBy", sortBy);
    params.set("page", String(page));
    params.set("size", String(pageSize));
    return `/inventory${params.toString() ? `?${params.toString()}` : ""}`;
  }, [page, pageSize, query, sortBy, status]);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<InventoryPageResponse>(endpoint);
      setItems(response.data.content);
      setPage(response.data.page);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch {
      setError("Unable to load inventory. Please check the backend connection.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [endpoint]);

  const openAdjust = (item: InventoryItem) => {
    setAdjusting(item);
    setAdjustForm({
      adjustmentType: "ADD_STOCK",
      quantity: "1",
      reason: "",
      remarks: "",
      transactionDate: new Date().toISOString().slice(0, 10),
    });
  };

  const submitAdjustment = async (event: FormEvent) => {
    event.preventDefault();
    if (!adjusting) return;
    setSaving(true);
    setError("");
    try {
      await api.post("/inventory/adjustments", {
        variantId: adjusting.variantId,
        adjustmentType: adjustForm.adjustmentType,
        quantity: Number(adjustForm.quantity),
        reason: adjustForm.reason,
        remarks: adjustForm.remarks || null,
        transactionDate: adjustForm.transactionDate,
      });
      setAdjusting(null);
      await load();
    } catch {
      setError("Unable to save adjustment. Check the quantity, reason, and current stock.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Inventory</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Stock dashboard</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Track live stock, low-stock items, and manual adjustments across SKU variants.
            </p>
          </div>
          <div className="rounded-lg border bg-card px-4 py-3 text-right">
            <p className="text-xs uppercase tracking-wider text-muted-foreground">Visible records</p>
            <p className="text-xl font-semibold">{totalElements}</p>
          </div>
        </div>

        <div className="mt-6 rounded-xl border bg-card shadow-sm">
          <div className="border-b p-4">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <form
                onSubmit={(event) => {
                  event.preventDefault();
                  setQuery(queryInput);
                  setPage(0);
                }}
                className="flex flex-1 flex-col gap-3 sm:flex-row"
              >
                <input
                  value={queryInput}
                  onChange={(event) => setQueryInput(event.target.value)}
                  placeholder="Search SKU, product, brand, category, color, or size"
                  className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-slate-300"
                />
                <button
                  type="submit"
                  className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
                >
                  Search
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setQueryInput("");
                    setQuery("");
                    setPage(0);
                  }}
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                >
                  Reset
                </button>
              </form>

              <div className="flex flex-wrap gap-2">
                {statusFilters.map((filter) => (
                  <button
                    key={filter.value || "ALL"}
                    type="button"
                    onClick={() => {
                      setStatus(filter.value);
                      setPage(0);
                    }}
                    className={`rounded-full px-3 py-1.5 text-sm font-medium transition ${
                      status === filter.value ? "bg-slate-900 text-white" : "border bg-background hover:bg-muted"
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-3">
              <label className="text-sm font-medium text-muted-foreground">
                Sort by
                <select
                  value={sortBy}
                  onChange={(event) => {
                    setSortBy(event.target.value);
                    setPage(0);
                  }}
                  className="ml-3 rounded-md border bg-background px-3 py-2 text-sm"
                >
                  {sortOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
              <p className="text-sm text-muted-foreground">
                Page {page + 1} of {Math.max(totalPages, 1)}
              </p>
            </div>
          </div>

          {error && <p className="m-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}

          {loading ? (
            <p className="p-6 text-sm text-muted-foreground">Loading inventory...</p>
          ) : items.length === 0 ? (
            <p className="p-6 text-sm text-muted-foreground">No inventory records found for the current filter.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted text-muted-foreground">
                  <tr>
                    <th className="px-5 py-3 font-medium">SKU</th>
                    <th className="px-5 py-3 font-medium">Product</th>
                    <th className="px-5 py-3 font-medium">Variant</th>
                    <th className="px-5 py-3 font-medium">Stock</th>
                    <th className="px-5 py-3 font-medium">Value</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                    <th className="px-5 py-3 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.variantId} className="border-t">
                      <td className="px-5 py-3 font-medium">{item.sku}</td>
                      <td className="px-5 py-3">
                        <div className="font-medium text-foreground">{item.productName}</div>
                        <div className="text-xs text-muted-foreground">
                          {item.brandName} - {item.categoryName}
                        </div>
                      </td>
                      <td className="px-5 py-3 text-muted-foreground">
                        {item.color} - {item.size}
                      </td>
                      <td className="px-5 py-3">
                        <div className="font-medium">{item.currentStock}</div>
                        <div className="text-xs text-muted-foreground">Reorder at {item.lowStockThreshold}</div>
                      </td>
                      <td className="px-5 py-3">{formatCurrency(item.inventoryValue)}</td>
                      <td className="px-5 py-3">
                        <span
                          className={`rounded-full px-2 py-1 text-xs font-medium ${
                            item.status === "OUT_OF_STOCK"
                              ? "bg-red-100 text-red-700"
                              : item.status === "LOW_STOCK"
                                ? "bg-amber-100 text-amber-800"
                                : "bg-emerald-100 text-emerald-700"
                          }`}
                        >
                          {item.status.replaceAll("_", " ")}
                        </span>
                      </td>
                      <td className="space-x-3 px-5 py-3 text-right">
                        <Link href={`/inventory/${item.variantId}`} className="text-slate-700 hover:underline">
                          View
                        </Link>
                        <button onClick={() => openAdjust(item)} className="text-slate-700 hover:underline">
                          Adjust
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="flex items-center justify-between border-t px-4 py-3 text-sm text-muted-foreground">
            <p>
              Showing {items.length} of {totalElements} records
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((current) => Math.max(current - 1, 0))}
                className="rounded-md border px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Previous
              </button>
              <button
                type="button"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((current) => current + 1)}
                className="rounded-md border px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      </section>

      {adjusting && (
        <div className="fixed inset-0 z-20 grid place-items-center bg-slate-950/40 p-4">
          <form onSubmit={submitAdjustment} className="w-full max-w-2xl rounded-xl bg-card p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-lg font-semibold">Adjust stock</h2>
                <p className="text-sm text-muted-foreground">
                  {adjusting.sku} - {adjusting.productName}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setAdjusting(null)}
                className="text-sm text-muted-foreground hover:text-foreground"
              >
                Close
              </button>
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <label className="text-sm font-medium">
                Adjustment type
                <select
                  value={adjustForm.adjustmentType}
                  onChange={(event) => setAdjustForm({ ...adjustForm, adjustmentType: event.target.value as AdjustmentType })}
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
                  value={adjustForm.quantity}
                  onChange={(event) => setAdjustForm({ ...adjustForm, quantity: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                />
              </label>
              <label className="text-sm font-medium">
                Reason
                <input
                  required
                  value={adjustForm.reason}
                  onChange={(event) => setAdjustForm({ ...adjustForm, reason: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                  placeholder="Damage, audit correction, found stock, etc."
                />
              </label>
              <label className="text-sm font-medium">
                Transaction date
                <input
                  required
                  type="date"
                  value={adjustForm.transactionDate}
                  onChange={(event) => setAdjustForm({ ...adjustForm, transactionDate: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                />
              </label>
              <label className="text-sm font-medium md:col-span-2">
                Remarks
                <textarea
                  value={adjustForm.remarks}
                  onChange={(event) => setAdjustForm({ ...adjustForm, remarks: event.target.value })}
                  className="mt-1 min-h-24 w-full rounded-md border bg-background px-3 py-2"
                  placeholder="Optional context for the adjustment"
                />
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={() => setAdjusting(null)} className="rounded-md border px-4 py-2 text-sm font-medium">
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
