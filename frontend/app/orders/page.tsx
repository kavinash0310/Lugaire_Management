"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import { fetchMarketplaces, type Marketplace } from "@/lib/marketplaces";

type OrderRow = {
  id: number;
  orderId: string;
  platform: string;
  orderDate: string;
  orderStatus: string;
  paymentStatus: string;
};

type Page = {
  content: OrderRow[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Page>({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [marketplaces, setMarketplaces] = useState<Marketplace[]>([]);
  const [search, setSearch] = useState("");
  const [marketplaceId, setMarketplaceId] = useState("");
  const [status, setStatus] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchMarketplaces().then(setMarketplaces).catch(() => setMarketplaces([]));
  }, []);

  const query = useMemo(() => {
    const params = new URLSearchParams({ search, page: String(currentPage), size: "20" });
    if (marketplaceId) params.set("marketplaceId", marketplaceId);
    if (status) params.set("status", status);
    return params.toString();
  }, [search, currentPage, marketplaceId, status]);

  useEffect(() => {
    setLoading(true);
    setError("");
    api.get<Page>(`/orders?${query}`)
      .then((response) => setOrders(response.data))
      .catch(() => setError("Unable to load orders."))
      .finally(() => setLoading(false));
  }, [query]);

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Sales</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Orders</h1>
          </div>
          <Link href="/orders/new" className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800">
            Add order
          </Link>
        </div>

        <div className="mt-6 grid gap-3 rounded-lg border bg-card p-4 md:grid-cols-3">
          <input
            value={search}
            onChange={(event) => { setSearch(event.target.value); setCurrentPage(0); }}
            placeholder="Search order ID"
            className="rounded border px-3 py-2"
          />
          <select
            value={marketplaceId}
            onChange={(event) => { setMarketplaceId(event.target.value); setCurrentPage(0); }}
            className="rounded border bg-background px-3 py-2"
          >
            <option value="">All marketplaces</option>
            {marketplaces.map((marketplace) => (
              <option key={marketplace.id} value={marketplace.id}>
                {marketplace.code} · {marketplace.name}
              </option>
            ))}
          </select>
          <select
            value={status}
            onChange={(event) => { setStatus(event.target.value); setCurrentPage(0); }}
            className="rounded border bg-background px-3 py-2"
          >
            <option value="">All statuses</option>
            {["PENDING", "PROCESSING", "ON_THE_WAY", "DELIVERED", "RETURNED", "RTO", "CANCELLED"].map((value) => (
              <option key={value} value={value}>{value}</option>
            ))}
          </select>
        </div>

        {loading && <p className="mt-6 text-sm text-muted-foreground">Loading orders...</p>}
        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {!loading && !error && (
          <>
            {orders.content.length === 0 ? (
              <p className="mt-6 rounded border bg-card p-6 text-sm text-muted-foreground">No orders found.</p>
            ) : (
              <div className="mt-6 overflow-x-auto rounded-lg border bg-card shadow-sm">
                <table className="w-full text-left text-sm">
                  <thead className="bg-muted text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3">Order ID</th>
                      <th className="px-4 py-3">Marketplace</th>
                      <th className="px-4 py-3">Date</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3">Payment</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.content.map((order) => (
                      <tr key={order.id} className="border-t">
                        <td className="px-4 py-3 font-medium">
                          <Link className="underline" href={`/orders/${order.id}`}>
                            {order.orderId}
                          </Link>
                        </td>
                        <td className="px-4 py-3">{order.platform}</td>
                        <td className="px-4 py-3">{order.orderDate}</td>
                        <td className="px-4 py-3">{order.orderStatus}</td>
                        <td className="px-4 py-3">{order.paymentStatus}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="mt-4 flex items-center gap-3">
              <button
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((page) => page - 1)}
                className="rounded border px-3 py-2 disabled:opacity-50"
              >
                Previous
              </button>
              <span className="text-sm">Page {orders.totalPages === 0 ? 0 : currentPage + 1} of {orders.totalPages}</span>
              <button
                disabled={currentPage + 1 >= orders.totalPages}
                onClick={() => setCurrentPage((page) => page + 1)}
                className="rounded border px-3 py-2 disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </>
        )}
      </section>
    </AppShell>
  );
}
