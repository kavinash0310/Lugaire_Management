"use client";

import { type ReactNode, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type ReportResponse = {
  period: string;
  fromDate: string;
  toDate: string;
  revenue: {
    grossSales: number;
    deliveredSales: number;
    returnedSales: number;
    rtoSales: number;
    cancelledSales: number;
    orders: number;
    deliveredOrders: number;
    returnedOrders: number;
    rtoOrders: number;
    cancelledOrders: number;
  };
  costs: {
    productCost: number;
    returnLoss: number;
    marketplaceFees: number;
    shippingCharges: number;
    returnCharges: number;
    otherCharges: number;
    settlementReceived: number;
    settlementDifference: number;
  };
  expenses: {
    totalExpenses: number;
    paidExpenses: number;
    pendingExpenses: number;
  };
  profit: {
    grossProfit: number;
    netProfit: number;
    profitMargin: number;
  };
  marketplaces: Array<{
    platform: string;
    orders: number;
    sales: number;
    marketplaceFees: number;
    shippingCharges: number;
    returnCharges: number;
    settlements: number;
    netRevenue: number;
    profit: number;
  }>;
  expenseBreakdown: Array<{ categoryId: number; categoryName: string; amount: number; percentage: number }>;
  products: Array<{ productId: number; productName: string; sku: string; unitsSold: number; revenue: number; productCost: number; returnsLoss: number; profit: number }>;
};

const periods = ["TODAY", "THIS_WEEK", "THIS_MONTH", "PREVIOUS_MONTH", "CUSTOM"] as const;

export default function ReportsPage() {
  const [period, setPeriod] = useState<(typeof periods)[number]>("THIS_MONTH");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [report, setReport] = useState<ReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const query = useMemo(() => {
    const params = new URLSearchParams({ period });
    if (period === "CUSTOM") {
      if (fromDate) params.set("fromDate", fromDate);
      if (toDate) params.set("toDate", toDate);
    }
    return params.toString();
  }, [period, fromDate, toDate]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await api.get<ReportResponse>(`/reports/financial?${query}`);
        setReport(response.data);
      } catch {
        setError("Unable to load report data.");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [query]);

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Reports</h1>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <select value={period} onChange={(event) => setPeriod(event.target.value as typeof period)} className="rounded border bg-background px-3 py-2 text-sm">
              {periods.map((value) => <option key={value} value={value}>{value.replaceAll("_", " ")}</option>)}
            </select>
            {period === "CUSTOM" && (
              <>
                <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} className="rounded border px-3 py-2 text-sm" />
                <input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} className="rounded border px-3 py-2 text-sm" />
              </>
            )}
          </div>
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {loading ? <p className="mt-6 text-sm text-muted-foreground">Loading report...</p> : !report ? null : (
          <div className="mt-6 space-y-8">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <Metric title="Gross Sales" value={report.revenue.grossSales} />
              <Metric title="Net Profit" value={report.profit.netProfit} />
              <Metric title="Total Expenses" value={report.expenses.totalExpenses} />
              <Metric title="Profit Margin" value={`${report.profit.profitMargin.toFixed(2)}%`} plain />
              <Metric title="Product Cost" value={report.costs.productCost} />
              <Metric title="Marketplace Charges" value={report.costs.marketplaceFees + report.costs.shippingCharges + report.costs.returnCharges + report.costs.otherCharges} />
              <Metric title="Returns / RTO Loss" value={report.costs.returnLoss} />
              <Metric title="Settlement Received" value={report.costs.settlementReceived} />
            </div>

            <section className="grid gap-6 xl:grid-cols-2">
              <Panel title="Revenue">
                <KeyValue label="Orders" value={report.revenue.orders.toString()} plain />
                <KeyValue label="Delivered Sales" value={report.revenue.deliveredSales} />
                <KeyValue label="Returned Sales" value={report.revenue.returnedSales} />
                <KeyValue label="RTO Sales" value={report.revenue.rtoSales} />
                <KeyValue label="Cancelled Sales" value={report.revenue.cancelledSales} />
              </Panel>
              <Panel title="Costs">
                <KeyValue label="Product Cost" value={report.costs.productCost} />
                <KeyValue label="Marketplace Fees" value={report.costs.marketplaceFees} />
                <KeyValue label="Shipping Charges" value={report.costs.shippingCharges} />
                <KeyValue label="Return Charges" value={report.costs.returnCharges} />
                <KeyValue label="Other Charges" value={report.costs.otherCharges} />
                <KeyValue label="Return / RTO Loss" value={report.costs.returnLoss} />
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-2">
              <Panel title="Expense Breakdown">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-muted text-muted-foreground">
                      <tr>
                        <th className="px-4 py-3">Category</th>
                        <th className="px-4 py-3">Amount</th>
                        <th className="px-4 py-3">%</th>
                      </tr>
                    </thead>
                    <tbody>
                      {report.expenseBreakdown.map((item) => (
                        <tr key={item.categoryId} className="border-t">
                          <td className="px-4 py-3">{item.categoryName}</td>
                          <td className="px-4 py-3">{money(item.amount)}</td>
                          <td className="px-4 py-3">{item.percentage.toFixed(2)}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
              <Panel title="Marketplace Performance">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-muted text-muted-foreground">
                      <tr>
                        <th className="px-4 py-3">Platform</th>
                        <th className="px-4 py-3">Orders</th>
                        <th className="px-4 py-3">Sales</th>
                        <th className="px-4 py-3">Fees</th>
                        <th className="px-4 py-3">Net Revenue</th>
                        <th className="px-4 py-3">Profit</th>
                      </tr>
                    </thead>
                    <tbody>
                      {report.marketplaces.map((item) => (
                        <tr key={item.platform} className="border-t">
                          <td className="px-4 py-3">{item.platform}</td>
                          <td className="px-4 py-3">{item.orders}</td>
                          <td className="px-4 py-3">{money(item.sales)}</td>
                          <td className="px-4 py-3">{money(item.marketplaceFees + item.shippingCharges + item.returnCharges)}</td>
                          <td className="px-4 py-3">{money(item.netRevenue)}</td>
                          <td className="px-4 py-3">{money(item.profit)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
            </section>

            <Panel title="Product Performance">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-muted text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3">Product</th>
                      <th className="px-4 py-3">SKU</th>
                      <th className="px-4 py-3">Units Sold</th>
                      <th className="px-4 py-3">Revenue</th>
                      <th className="px-4 py-3">Product Cost</th>
                      <th className="px-4 py-3">Returns</th>
                      <th className="px-4 py-3">Profit</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.products.map((item) => (
                      <tr key={item.sku} className="border-t">
                        <td className="px-4 py-3">{item.productName}</td>
                        <td className="px-4 py-3">{item.sku}</td>
                        <td className="px-4 py-3">{item.unitsSold}</td>
                        <td className="px-4 py-3">{money(item.revenue)}</td>
                        <td className="px-4 py-3">{money(item.productCost)}</td>
                        <td className="px-4 py-3">{money(item.returnsLoss)}</td>
                        <td className="px-4 py-3">{money(item.profit)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Panel>
          </div>
        )}
      </section>
    </AppShell>
  );
}

function Metric({ title, value, plain = false }: { title: string; value: number | string; plain?: boolean }) {
  return (
    <div className="rounded-lg border bg-card p-4 shadow-sm">
      <p className="text-sm text-muted-foreground">{title}</p>
      <p className="mt-2 text-2xl font-semibold">{plain ? value : typeof value === "number" ? money(value) : value}</p>
    </div>
  );
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border bg-card p-5 shadow-sm">
      <h2 className="text-lg font-semibold">{title}</h2>
      <div className="mt-4">{children}</div>
    </div>
  );
}

function KeyValue({ label, value, plain = false }: { label: string; value: number | string; plain?: boolean }) {
  return (
    <div className="flex items-center justify-between border-b py-2 text-sm last:border-b-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{plain ? value : typeof value === "number" ? money(value) : value}</span>
    </div>
  );
}

function money(value: number) {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(value || 0);
}
