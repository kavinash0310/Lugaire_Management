"use client";

import { type FormEvent, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type MasterItem = {
  id: number;
  name: string;
  code: string;
  active: boolean;
};

type PricingItem = {
  variantId: number;
  sku: string;
  productName: string;
  brandName: string;
  categoryName: string;
  colorName: string;
  sizeName: string;
  active: boolean;
  unitCost: string;
  sellingPrice: string;
  grossProfit: string;
  marginPercent: string;
  currentStock: number;
  inventoryValue: string;
};

type PricingPageResponse = {
  content: PricingItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type PricingHistory = {
  id: number;
  previousSellingPrice: string;
  newSellingPrice: string;
  effectiveDate: string;
  remarks: string | null;
  userId: number | null;
  userName: string | null;
  createdAt: string;
};

type PricingDetail = {
  summary: PricingItem;
  history: PricingHistory[];
};

function money(value: string | number | null) {
  if (value === null || value === undefined) return "-";
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return String(value);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(numeric);
}

function percent(value: string | number | null) {
  if (value === null || value === undefined) return "-";
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return String(value);
  return `${numeric.toFixed(2)}%`;
}

export default function PricingPage() {
  const [items, setItems] = useState<PricingItem[]>([]);
  const [brands, setBrands] = useState<MasterItem[]>([]);
  const [categories, setCategories] = useState<MasterItem[]>([]);
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [brandId, setBrandId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [sortBy, setSortBy] = useState("SKU_ASC");
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [editItem, setEditItem] = useState<PricingItem | null>(null);
  const [historyItem, setHistoryItem] = useState<PricingDetail | null>(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [form, setForm] = useState({
    sellingPrice: "",
    effectiveDate: new Date().toISOString().slice(0, 10),
    remarks: "",
  });

  const endpoint = useMemo(() => {
    const params = new URLSearchParams();
    if (query.trim()) params.set("query", query.trim());
    if (brandId) params.set("brandId", brandId);
    if (categoryId) params.set("categoryId", categoryId);
    if (sortBy) params.set("sortBy", sortBy);
    params.set("page", String(page));
    params.set("size", String(pageSize));
    return `/pricing${params.toString() ? `?${params.toString()}` : ""}`;
  }, [brandId, categoryId, page, pageSize, query, sortBy]);

  const loadFilters = async () => {
    const [brandResponse, categoryResponse] = await Promise.all([
      api.get<MasterItem[]>("/brands"),
      api.get<MasterItem[]>("/categories"),
    ]);
    setBrands(brandResponse.data.filter((item) => item.active));
    setCategories(categoryResponse.data.filter((item) => item.active));
  };

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<PricingPageResponse>(endpoint);
      setItems(response.data.content);
      setPage(response.data.page);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch {
      setError("Unable to load pricing. Please check the backend connection.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadFilters().catch(() => setError("Unable to load brand and category filters."));
  }, []);

  useEffect(() => {
    void load();
  }, [endpoint]);

  const openEdit = (item: PricingItem) => {
    setEditItem(item);
    setForm({
      sellingPrice: String(item.sellingPrice),
      effectiveDate: new Date().toISOString().slice(0, 10),
      remarks: "",
    });
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!editItem) return;
    setSaving(true);
    setError("");
    try {
      await api.put(`/pricing/${editItem.variantId}`, {
        sellingPrice: Number(form.sellingPrice),
        effectiveDate: form.effectiveDate,
        remarks: form.remarks || null,
      });
      setEditItem(null);
      await load();
    } catch {
      setError("Unable to update price. Check the price value and try again.");
    } finally {
      setSaving(false);
    }
  };

  const openHistory = async (item: PricingItem) => {
    setHistoryLoading(true);
    setHistoryItem(null);
    setError("");
    try {
      const response = await api.get<PricingDetail>(`/pricing/${item.variantId}`);
      setHistoryItem(response.data);
    } catch {
      setError("Unable to load pricing history.");
    } finally {
      setHistoryLoading(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Pricing</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">SKU cost and pricing</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Common SKU selling price, derived cost snapshot, gross profit, margin, and inventory value.
            </p>
          </div>
          <div className="rounded-lg border bg-card px-4 py-3 text-right">
            <p className="text-xs uppercase tracking-wider text-muted-foreground">Visible SKUs</p>
            <p className="text-xl font-semibold">{totalElements}</p>
          </div>
        </div>

        <div className="mt-6 rounded-xl border bg-card shadow-sm">
          <div className="border-b p-4">
            <div className="grid gap-3 lg:grid-cols-[1fr_auto_auto_auto]">
              <form
                onSubmit={(event) => {
                  event.preventDefault();
                  setQuery(queryInput);
                  setPage(0);
                }}
                className="flex gap-2"
              >
                <input
                  value={queryInput}
                  onChange={(event) => setQueryInput(event.target.value)}
                  placeholder="Search SKU, product, brand, category, color, or size"
                  className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                />
                <button className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white">Search</button>
              </form>
              <select
                value={brandId}
                onChange={(event) => {
                  setBrandId(event.target.value);
                  setPage(0);
                }}
                className="rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="">All brands</option>
                {brands.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
              <select
                value={categoryId}
                onChange={(event) => {
                  setCategoryId(event.target.value);
                  setPage(0);
                }}
                className="rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="">All categories</option>
                {categories.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
              <select
                value={sortBy}
                onChange={(event) => {
                  setSortBy(event.target.value);
                  setPage(0);
                }}
                className="rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="SKU_ASC">SKU</option>
                <option value="PRODUCT_ASC">Product</option>
                <option value="PRICE_ASC">Price low to high</option>
                <option value="PRICE_DESC">Price high to low</option>
                <option value="PROFIT_DESC">Highest profit</option>
                <option value="MARGIN_DESC">Highest margin</option>
              </select>
            </div>
          </div>

          {error && <p className="m-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}

          {loading ? (
            <p className="p-6 text-sm text-muted-foreground">Loading pricing...</p>
          ) : items.length === 0 ? (
            <p className="p-6 text-sm text-muted-foreground">No pricing records found for the current filter.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted text-muted-foreground">
                  <tr>
                    <th className="px-5 py-3 font-medium">SKU</th>
                    <th className="px-5 py-3 font-medium">Product</th>
                    <th className="px-5 py-3 font-medium">Variant</th>
                    <th className="px-5 py-3 font-medium">Unit cost</th>
                    <th className="px-5 py-3 font-medium">Selling price</th>
                    <th className="px-5 py-3 font-medium">Gross profit</th>
                    <th className="px-5 py-3 font-medium">Margin</th>
                    <th className="px-5 py-3 font-medium">Inventory</th>
                    <th className="px-5 py-3 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.variantId} className="border-t">
                      <td className="px-5 py-3 font-medium">
                        <div>{item.sku}</div>
                        <span
                          className={`mt-1 inline-flex rounded-full px-2 py-1 text-xs font-medium ${
                            item.active ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-600"
                          }`}
                        >
                          {item.active ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <div className="font-medium text-foreground">{item.productName}</div>
                        <div className="text-xs text-muted-foreground">
                          {item.brandName} - {item.categoryName}
                        </div>
                      </td>
                      <td className="px-5 py-3 text-muted-foreground">
                        {item.colorName} - {item.sizeName}
                      </td>
                      <td className="px-5 py-3">{money(item.unitCost)}</td>
                      <td className="px-5 py-3">{money(item.sellingPrice)}</td>
                      <td className="px-5 py-3">{money(item.grossProfit)}</td>
                      <td className="px-5 py-3">{percent(item.marginPercent)}</td>
                      <td className="px-5 py-3">
                        <div>{item.currentStock}</div>
                        <div className="text-xs text-muted-foreground">{money(item.inventoryValue)}</div>
                      </td>
                      <td className="space-x-3 px-5 py-3 text-right">
                        <button onClick={() => openEdit(item)} className="text-slate-700 hover:underline">
                          Edit
                        </button>
                        <button onClick={() => void openHistory(item)} className="text-slate-700 hover:underline">
                          History
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
              Showing {items.length} of {totalElements} SKUs
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

        <p className="mt-4 text-sm text-muted-foreground">
          Cost is displayed from the current SKU cost snapshot. Actual marketplace charges and settlement amounts remain in the settlement module.
        </p>
      </section>

      {editItem && (
        <div className="fixed inset-0 z-20 grid place-items-center bg-slate-950/40 p-4">
          <form onSubmit={submit} className="w-full max-w-xl rounded-xl bg-card p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-lg font-semibold">Edit selling price</h2>
                <p className="text-sm text-muted-foreground">
                  {editItem.sku} - {editItem.productName}
                </p>
              </div>
              <button type="button" onClick={() => setEditItem(null)} className="text-sm text-muted-foreground hover:text-foreground">
                Close
              </button>
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <label className="text-sm font-medium">
                Selling price
                <input
                  required
                  min="0"
                  type="number"
                  step="0.01"
                  value={form.sellingPrice}
                  onChange={(event) => setForm({ ...form, sellingPrice: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                />
              </label>
              <label className="text-sm font-medium">
                Effective date
                <input
                  required
                  type="date"
                  value={form.effectiveDate}
                  onChange={(event) => setForm({ ...form, effectiveDate: event.target.value })}
                  className="mt-1 w-full rounded-md border bg-background px-3 py-2"
                />
              </label>
              <label className="text-sm font-medium md:col-span-2">
                Remarks
                <textarea
                  value={form.remarks}
                  onChange={(event) => setForm({ ...form, remarks: event.target.value })}
                  className="mt-1 min-h-24 w-full rounded-md border bg-background px-3 py-2"
                  placeholder="Optional reason or pricing note"
                />
              </label>
            </div>

            <div className="mt-6 flex items-center justify-between gap-3">
              <div className="text-sm text-muted-foreground">
                Gross profit will update automatically after saving.
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => setEditItem(null)} className="rounded-md border px-4 py-2 text-sm font-medium">
                  Cancel
                </button>
                <button disabled={saving} className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50">
                  {saving ? "Saving..." : "Save price"}
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      {historyLoading && (
        <div className="fixed inset-0 z-30 grid place-items-center bg-slate-950/30 p-4">
          <div className="rounded-xl bg-card p-6 shadow-xl">Loading pricing history...</div>
        </div>
      )}

      {historyItem && (
        <div className="fixed inset-0 z-20 grid place-items-center bg-slate-950/40 p-4">
          <div className="w-full max-w-3xl rounded-xl bg-card p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-lg font-semibold">Price history</h2>
                <p className="text-sm text-muted-foreground">
                  {historyItem.summary.sku} - {historyItem.summary.productName}
                </p>
              </div>
              <button type="button" onClick={() => setHistoryItem(null)} className="text-sm text-muted-foreground hover:text-foreground">
                Close
              </button>
            </div>

            <div className="mt-4 grid gap-4 md:grid-cols-4">
              <Stat label="Current price" value={money(historyItem.summary.sellingPrice)} />
              <Stat label="Cost" value={money(historyItem.summary.unitCost)} />
              <Stat label="Gross profit" value={money(historyItem.summary.grossProfit)} />
              <Stat label="Margin" value={percent(historyItem.summary.marginPercent)} />
            </div>

            <div className="mt-6 overflow-x-auto rounded-lg border">
              {historyItem.history.length === 0 ? (
                <p className="p-4 text-sm text-muted-foreground">No price changes recorded yet.</p>
              ) : (
                <table className="w-full text-left text-sm">
                  <thead className="bg-muted text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3 font-medium">Date</th>
                      <th className="px-4 py-3 font-medium">Previous</th>
                      <th className="px-4 py-3 font-medium">New</th>
                      <th className="px-4 py-3 font-medium">Remarks</th>
                      <th className="px-4 py-3 font-medium">User</th>
                    </tr>
                  </thead>
                  <tbody>
                    {historyItem.history.map((item) => (
                      <tr key={item.id} className="border-t">
                        <td className="px-4 py-3">
                          <div>{item.effectiveDate}</div>
                          <div className="text-xs text-muted-foreground">{new Date(item.createdAt).toLocaleString("en-IN")}</div>
                        </td>
                        <td className="px-4 py-3">{money(item.previousSellingPrice)}</td>
                        <td className="px-4 py-3">{money(item.newSellingPrice)}</td>
                        <td className="px-4 py-3">{item.remarks ?? "-"}</td>
                        <td className="px-4 py-3">
                          <div>{item.userName ?? "SYSTEM"}</div>
                          <div className="text-xs text-muted-foreground">{item.userId ?? "-"}</div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-background p-4">
      <p className="text-xs uppercase tracking-wider text-muted-foreground">{label}</p>
      <p className="mt-2 text-lg font-semibold">{value}</p>
    </div>
  );
}
