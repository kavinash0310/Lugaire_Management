"use client";

import Link from "next/link";
import { type ReactNode, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type DashboardReport = {
  revenue: { grossSales: number; deliveredSales: number; returnedSales: number; rtoSales: number };
  costs: { productCost: number; marketplaceFees: number; shippingCharges: number; returnCharges: number; otherCharges: number; returnLoss: number };
  expenses: { totalExpenses: number; pendingExpenses: number };
  profit: { netProfit: number; profitMargin: number };
  expenseBreakdown: Array<{ categoryId: number; categoryName: string; amount: number }>;
};

export function DashboardShell() {
  const [report, setReport] = useState<DashboardReport | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<DashboardReport>("/reports/financial?period=THIS_MONTH")
      .then((response) => setReport(response.data))
      .catch(() => setError("Unable to load dashboard metrics."));
  }, []);

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Overview</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Dashboard</h1>
          </div>
          <Link href="/reports" className="rounded-md border px-4 py-2 text-sm font-medium">View reports</Link>
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Metric title="Total Sales" value={report ? report.revenue.grossSales : null} />
          <Metric title="Net Profit" value={report ? report.profit.netProfit : null} />
          <Metric title="Total Expenses" value={report ? report.expenses.totalExpenses : null} />
          <Metric title="Profit Margin" value={report ? `${report.profit.profitMargin}%` : null} plain />
          <Metric title="Product Cost" value={report ? report.costs.productCost : null} />
          <Metric title="Marketplace Charges" value={report ? report.costs.marketplaceFees + report.costs.shippingCharges + report.costs.returnCharges + report.costs.otherCharges : null} />
          <Metric title="Returns / RTO Loss" value={report ? report.costs.returnLoss : null} />
          <Metric title="Pending Expenses" value={report ? report.expenses.pendingExpenses : null} />
        </div>

        <div className="mt-8 grid gap-6 xl:grid-cols-2">
          <Panel title="Revenue Snapshot" description="This month’s sales and order mix.">
            <KeyValue label="Gross sales" value={report?.revenue.grossSales ?? null} />
            <KeyValue label="Delivered sales" value={report?.revenue.deliveredSales ?? null} />
            <KeyValue label="Returned sales" value={report?.revenue.returnedSales ?? null} />
            <KeyValue label="RTO sales" value={report?.revenue.rtoSales ?? null} />
          </Panel>
          <Panel title="Top Expense Categories" description="Operating costs by category.">
            <div className="space-y-3">
              {(report?.expenseBreakdown?.slice(0, 5) ?? []).map((item) => (
                <div key={item.categoryId} className="flex items-center justify-between gap-3 text-sm">
                  <span>{item.categoryName}</span>
                  <span className="font-medium">{formatMoney(item.amount)}</span>
                </div>
              ))}
            </div>
          </Panel>
        </div>
      </section>
    </AppShell>
  );
}

function Metric({ title, value, plain = false }: { title: string; value: number | string | null; plain?: boolean }) {
  return (
    <div className="rounded-lg border bg-card p-4 shadow-sm">
      <p className="text-sm text-muted-foreground">{title}</p>
      <p className="mt-2 text-2xl font-semibold">{plain ? (value ?? "—") : formatMoney(value)}</p>
    </div>
  );
}

function Panel({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border bg-card p-5 shadow-sm">
      <h2 className="text-lg font-semibold">{title}</h2>
      <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      <div className="mt-4">{children}</div>
    </div>
  );
}

function KeyValue({ label, value }: { label: string; value: number | string | null }) {
  return (
    <div className="flex items-center justify-between border-b py-2 text-sm last:border-b-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{formatMoney(value)}</span>
    </div>
  );
}

function formatMoney(value: number | string | null) {
  if (value === null || value === undefined) {
    return "—";
  }
  if (typeof value === "string" && value.includes("%")) {
    return value;
  }
  const numeric = Number(value);
  if (Number.isNaN(numeric)) {
    return String(value);
  }
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(numeric);
}
