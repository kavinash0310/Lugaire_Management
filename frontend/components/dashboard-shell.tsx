"use client";

import Link from "next/link";
import { type ReactNode, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import { type DashboardSummary, type DashboardTrendPoint, type DashboardStatusBreakdown, type DashboardMarketplace, type DashboardInventory } from "@/lib/dashboard";

type FilterState = {
  period: "TODAY" | "THIS_WEEK" | "THIS_MONTH" | "PREVIOUS_MONTH" | "CUSTOM";
  fromDate: string;
  toDate: string;
};

const presets: Array<FilterState["period"]> = ["TODAY", "THIS_WEEK", "THIS_MONTH", "PREVIOUS_MONTH"];

export function DashboardShell() {
  const [draftFilters, setDraftFilters] = useState<FilterState>({ period: "THIS_MONTH", fromDate: "", toDate: "" });
  const [appliedFilters, setAppliedFilters] = useState<FilterState>({ period: "THIS_MONTH", fromDate: "", toDate: "" });
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");

  const query = useMemo(() => {
    const params = new URLSearchParams();
    params.set("period", appliedFilters.period);
    if (appliedFilters.period === "CUSTOM") {
      params.set("fromDate", appliedFilters.fromDate);
      params.set("toDate", appliedFilters.toDate);
    }
    return params.toString();
  }, [appliedFilters]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await api.get<DashboardSummary>(`/dashboard/summary?${query}`);
        setDashboard(response.data);
      } catch {
        setError("Unable to load dashboard data.");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [query]);

  const applyPreset = (period: FilterState["period"]) => {
    const next = { period, fromDate: "", toDate: "" };
    setDraftFilters(next);
    setAppliedFilters(next);
    setFormError("");
  };

  const applyCustom = () => {
    if (!draftFilters.fromDate || !draftFilters.toDate) {
      setFormError("Please select both dates for the custom range.");
      return;
    }
    if (draftFilters.toDate < draftFilters.fromDate) {
      setFormError("End date must be on or after start date.");
      return;
    }
    setFormError("");
    setAppliedFilters({ ...draftFilters, period: "CUSTOM" });
  };

  const hasActivity =
    !!dashboard &&
    (dashboard.revenue.orders > 0 ||
      dashboard.revenue.grossSales > 0 ||
      dashboard.expenses.totalExpenses > 0 ||
      dashboard.lowStock.length > 0 ||
      dashboard.marketplaces.length > 0 ||
      dashboard.expenseBreakdown.length > 0 ||
      dashboard.orderStatusBreakdown.some((item) => item.count > 0) ||
      dashboard.salesTrend.some((item) => item.value > 0) ||
      dashboard.profitTrend.some((item) => item.value !== 0));
  const empty = !dashboard || !hasActivity;

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Overview</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">{dashboard?.businessName ?? "Dashboard"}</h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Final operational view for sales, stock, settlements, returns, expenses, and profit.
            </p>
          </div>
          <Link href="/reports" className="rounded-md border px-4 py-2 text-sm font-medium">
            View reports
          </Link>
        </div>

        <div className="mt-6 rounded-xl border bg-card p-4 shadow-sm">
          <div className="flex flex-wrap items-center gap-2">
            {presets.map((period) => (
              <button
                key={period}
                type="button"
                onClick={() => applyPreset(period)}
                className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                  draftFilters.period === period && draftFilters.period !== "CUSTOM"
                    ? "bg-slate-900 text-white"
                    : "border bg-background hover:bg-muted"
                }`}
              >
                {presetLabel(period)}
              </button>
            ))}
            <button
              type="button"
              onClick={() => setDraftFilters((current) => ({ ...current, period: "CUSTOM" }))}
              className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                draftFilters.period === "CUSTOM" ? "bg-slate-900 text-white" : "border bg-background hover:bg-muted"
              }`}
            >
              Custom Range
            </button>
          </div>
          {draftFilters.period === "CUSTOM" && (
            <div className="mt-4 flex flex-wrap items-end gap-3">
              <label className="space-y-1">
                <span className="block text-xs font-medium uppercase text-muted-foreground">From</span>
                <input
                  type="date"
                  value={draftFilters.fromDate}
                  onChange={(event) => setDraftFilters((current) => ({ ...current, fromDate: event.target.value, period: "CUSTOM" }))}
                  className="rounded-md border bg-background px-3 py-2 text-sm"
                />
              </label>
              <label className="space-y-1">
                <span className="block text-xs font-medium uppercase text-muted-foreground">To</span>
                <input
                  type="date"
                  value={draftFilters.toDate}
                  onChange={(event) => setDraftFilters((current) => ({ ...current, toDate: event.target.value, period: "CUSTOM" }))}
                  className="rounded-md border bg-background px-3 py-2 text-sm"
                />
              </label>
              <button type="button" onClick={applyCustom} className="rounded-md bg-slate-900 px-4 py-2.5 text-sm font-medium text-white">
                Apply
              </button>
            </div>
          )}
          {formError && <p className="mt-3 text-sm text-red-700">{formError}</p>}
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {loading ? (
          <p className="mt-6 text-sm text-muted-foreground">Loading dashboard...</p>
        ) : empty ? (
          <p className="mt-6 rounded-lg border bg-card p-6 text-sm text-muted-foreground">No sales data for this period.</p>
        ) : dashboard ? (
          <>
            <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <KpiCard title="Total Orders" value={dashboard.revenue.orders} plain />
              <KpiCard title="Total Sales" value={dashboard.revenue.grossSales} currency={dashboard.currencySymbol} />
              <KpiCard title="Delivered Orders" value={dashboard.revenue.deliveredOrders} plain />
              <KpiCard title="Returned Orders" value={dashboard.revenue.returnedOrders} plain />
              <KpiCard title="RTO Orders" value={dashboard.revenue.rtoOrders} plain />
              <KpiCard
                title="Pending Settlements / Payments"
                value={dashboard.backlog.pendingPayments + dashboard.backlog.pendingSettlements}
                plain
                subtitle={`Payments ${dashboard.backlog.pendingPayments} • Settlements ${dashboard.backlog.pendingSettlements}`}
              />
              <KpiCard title="Total Expenses" value={dashboard.expenses.totalExpenses} currency={dashboard.currencySymbol} />
              <KpiCard title="Net Profit" value={dashboard.profit.netProfit} currency={dashboard.currencySymbol} />
            </div>

            <div className="mt-8 grid gap-6 xl:grid-cols-2">
              <ChartCard title="Sales Trend" description="Sales by day for the selected period.">
                <BarTrend data={dashboard.salesTrend} currency={dashboard.currencySymbol} />
              </ChartCard>
              <ChartCard title="Order Status" description="Current order mix for the selected period.">
                <StatusBreakdown statuses={dashboard.orderStatusBreakdown} />
              </ChartCard>
            </div>

            <div className="mt-6 grid gap-6 xl:grid-cols-2">
              <ChartCard title="Marketplace Performance" description="Platform-wise orders, sales, and settlement activity.">
                <MarketplaceTable marketplaces={dashboard.marketplaces} currency={dashboard.currencySymbol} />
              </ChartCard>
              <ChartCard title="Low Stock Products" description="Products that need replenishment.">
                <LowStockTable products={dashboard.lowStock} />
              </ChartCard>
            </div>

            <div className="mt-6 grid gap-6 xl:grid-cols-3">
              <ChartCard title="Returns / RTO" description="Return and RTO performance.">
                <div className="space-y-3 text-sm">
                  <MetricRow label="Return Count" value={dashboard.returns.returnCount} plain />
                  <MetricRow label="RTO Count" value={dashboard.returns.rtoCount} plain />
                  <MetricRow label="Return Rate" value={`${dashboard.returns.returnRate.toFixed(2)}%`} plain />
                  <MetricRow label="RTO Rate" value={`${dashboard.returns.rtoRate.toFixed(2)}%`} plain />
                </div>
              </ChartCard>
              <ChartCard title="Expenses" description="Total expenses and category split.">
                <div className="space-y-3 text-sm">
                  <MetricRow label="Total Expenses" value={dashboard.expenses.totalExpenses} currency={dashboard.currencySymbol} />
                  <MetricRow label="Paid" value={dashboard.expenses.paidExpenses} currency={dashboard.currencySymbol} />
                  <MetricRow label="Pending" value={dashboard.expenses.pendingExpenses} currency={dashboard.currencySymbol} />
                  <div className="mt-4 space-y-2">
                    {dashboard.expenseBreakdown.slice(0, 5).map((item) => (
                      <div key={item.categoryId}>
                        <div className="flex items-center justify-between gap-3 text-xs">
                          <span>{item.categoryName}</span>
                          <span>{item.percentage.toFixed(1)}%</span>
                        </div>
                        <div className="mt-1 h-2 rounded-full bg-muted">
                          <div className="h-2 rounded-full bg-slate-900" style={{ width: `${Math.min(item.percentage, 100)}%` }} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </ChartCard>
              <ChartCard title="Profit Trend" description="Profit over time for the selected period.">
                <BarTrend data={dashboard.profitTrend} currency={dashboard.currencySymbol} />
              </ChartCard>
            </div>

            <div className="mt-6 rounded-xl border bg-card p-5 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold">Profit Summary</h2>
                  <p className="text-sm text-muted-foreground">Realized financial performance from sales, costs, and expenses.</p>
                </div>
                <p className="text-xs text-muted-foreground">Updated {new Date(dashboard.generatedAt).toLocaleString()}</p>
              </div>
              <div className="mt-4 grid gap-4 md:grid-cols-3">
                <KpiCard title="Gross Profit" value={dashboard.profit.grossProfit} currency={dashboard.currencySymbol} />
                <KpiCard title="Net Profit" value={dashboard.profit.netProfit} currency={dashboard.currencySymbol} />
                <KpiCard title="Profit Margin" value={`${dashboard.profit.profitMargin.toFixed(2)}%`} plain />
              </div>
            </div>
          </>
        ) : null}
      </section>
    </AppShell>
  );
}

function presetLabel(period: FilterState["period"]) {
  return period === "TODAY" ? "Today" : period === "THIS_WEEK" ? "This Week" : period === "THIS_MONTH" ? "This Month" : "Last Month";
}

function KpiCard({ title, value, currency, subtitle, plain = false }: { title: string; value: number | string; currency?: string; subtitle?: string; plain?: boolean }) {
  return (
    <div className="rounded-xl border bg-card p-4 shadow-sm">
      <p className="text-sm text-muted-foreground">{title}</p>
      <p className="mt-2 text-2xl font-semibold">{plain ? formatCount(value) : formatMoney(value, currency)}</p>
      {subtitle ? <p className="mt-2 text-xs text-muted-foreground">{subtitle}</p> : null}
    </div>
  );
}

function ChartCard({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm">
      <h2 className="text-lg font-semibold">{title}</h2>
      <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      <div className="mt-4">{children}</div>
    </div>
  );
}

function BarTrend({ data, currency }: { data: DashboardTrendPoint[]; currency?: string }) {
  if (data.length === 0) {
    return <p className="text-sm text-muted-foreground">No data for this period.</p>;
  }
  const max = Math.max(...data.map((item) => item.value), 1);
  return (
    <div className="overflow-x-auto">
      <div className="flex min-w-[560px] items-end gap-2">
        {data.map((item) => (
          <div key={item.date} className="flex flex-1 flex-col items-center gap-2">
            <div className="flex h-56 w-full items-end rounded-md bg-muted/40 px-1">
              <div
                className="w-full rounded-t-md bg-slate-900"
                style={{ height: `${Math.max((item.value / max) * 100, item.value > 0 ? 4 : 0)}%` }}
                title={formatMoney(item.value, currency)}
              />
            </div>
            <div className="text-center text-[11px] text-muted-foreground">
              <div>{formatDateLabel(item.date)}</div>
              <div>{formatMoney(item.value, currency)}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function StatusBreakdown({ statuses }: { statuses: DashboardStatusBreakdown[] }) {
  if (statuses.length === 0) {
    return <p className="text-sm text-muted-foreground">No order data for this period.</p>;
  }
  const total = statuses.reduce((sum, item) => sum + item.count, 0) || 1;
  const colors = ["#0f172a", "#334155", "#64748b", "#22c55e", "#f59e0b", "#ef4444", "#8b5cf6"];
  const gradient = statuses
    .map((item, index) => {
      const start = statuses.slice(0, index).reduce((sum, current) => sum + (current.count / total) * 100, 0);
      const end = start + (item.count / total) * 100;
      return `${colors[index % colors.length]} ${start}% ${end}%`;
    })
    .join(", ");

  return (
    <div className="grid gap-4 md:grid-cols-[200px_1fr]">
      <div className="mx-auto flex size-48 items-center justify-center rounded-full border" style={{ background: `conic-gradient(${gradient})` }}>
        <div className="size-28 rounded-full bg-background" />
      </div>
      <div className="space-y-3 text-sm">
        {statuses.map((item, index) => {
          const percent = ((item.count / total) * 100).toFixed(1);
          return (
            <div key={item.status} className="space-y-1">
              <div className="flex items-center justify-between gap-3">
                <span>{displayStatus(item.status)}</span>
                <span className="font-medium">
                  {item.count} ({percent}%)
                </span>
              </div>
              <div className="h-2 rounded-full bg-muted">
                <div className="h-2 rounded-full" style={{ width: `${percent}%`, backgroundColor: colors[index % colors.length] }} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function MarketplaceTable({ marketplaces, currency }: { marketplaces: DashboardMarketplace[]; currency?: string }) {
  if (marketplaces.length === 0) {
    return <p className="text-sm text-muted-foreground">No marketplace performance data for this period.</p>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr>
            <th className="pb-3">Marketplace</th>
            <th className="pb-3">Orders</th>
            <th className="pb-3">Sales</th>
            <th className="pb-3">Returns / RTO</th>
            <th className="pb-3">Settled</th>
            <th className="pb-3">Profit</th>
          </tr>
        </thead>
        <tbody>
          {marketplaces.map((item) => (
            <tr key={item.platform} className="border-t">
              <td className="py-3 font-medium">{item.platform}</td>
              <td>{item.orders}</td>
              <td>{formatMoney(item.sales, currency)}</td>
              <td>{item.returnsRto}</td>
              <td>{formatMoney(item.settlements, currency)}</td>
              <td>{formatMoney(item.profit, currency)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function LowStockTable({ products }: { products: DashboardInventory[] }) {
  if (products.length === 0) {
    return <p className="text-sm text-muted-foreground">No low-stock products right now.</p>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr>
            <th className="pb-3">SKU</th>
            <th className="pb-3">Product</th>
            <th className="pb-3">Current Stock</th>
            <th className="pb-3">Threshold</th>
            <th className="pb-3">Status</th>
          </tr>
        </thead>
        <tbody>
          {products.map((item) => (
            <tr key={item.variantId} className="border-t">
              <td className="py-3 font-medium">{item.sku}</td>
              <td>{item.productName}</td>
              <td>{item.currentStock}</td>
              <td>{item.lowStockThreshold}</td>
              <td className={item.status === "OUT_OF_STOCK" ? "font-semibold text-red-600" : "font-semibold text-amber-600"}>{displayStatus(item.status)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MetricRow({ label, value, currency, plain = false }: { label: string; value: number | string; currency?: string; plain?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b py-2 last:border-b-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{plain ? formatCount(value) : formatMoney(value, currency)}</span>
    </div>
  );
}

function formatMoney(value: number | string, currency?: string) {
  if (typeof value === "string") {
    return value;
  }
  const symbol = currency ?? "₹";
  return `${symbol}${value.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatCount(value: number | string) {
  if (typeof value === "string") {
    return value;
  }
  return new Intl.NumberFormat("en-IN").format(value);
}

function formatDateLabel(value: string) {
  return new Date(value).toLocaleDateString("en-IN", { month: "short", day: "numeric" });
}

function displayStatus(value: string) {
  return value.replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}
