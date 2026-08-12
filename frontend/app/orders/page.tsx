"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Page = {
  content: Array<{ id: number; orderId: string; platform: string; orderDate: string; orderStatus: string; paymentStatus: string }>;
  page: number;
  totalPages: number;
  totalElements: number;
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Page>({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [search, setSearch] = useState("");
  const [platform, setPlatform] = useState("");
  const [status, setStatus] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");
    const query = new URLSearchParams({ search, page: String(currentPage), size: "20" });
    if (platform) query.set("platform", platform);
    if (status) query.set("status", status);
    api.get<Page>(`/orders?${query.toString()}`)
      .then((response) => setOrders(response.data))
      .catch(() => setError("Unable to load orders."))
      .finally(() => setLoading(false));
  }, [search, platform, status, currentPage]);

  return <AppShell><section className="mx-auto max-w-6xl p-5 md:p-8">
    <div className="flex justify-between"><div><p className="text-sm text-muted-foreground">Orders</p><h1 className="mt-1 text-2xl font-semibold">Order management</h1></div><Link href="/orders/new" className="rounded bg-slate-900 px-4 py-2 text-white">Add order</Link></div>
    <input value={search} onChange={(event) => { setSearch(event.target.value); setCurrentPage(0); }} placeholder="Search order ID" className="mt-6 w-full rounded border p-2" />
    <div className="mt-3 flex flex-wrap gap-3">
      <select value={platform} onChange={(event) => { setPlatform(event.target.value); setCurrentPage(0); }} className="rounded border bg-background p-2"><option value="">All platforms</option>{['MEESHO', 'AMAZON', 'FLIPKART', 'WEBSITE', 'OTHER'].map((value) => <option key={value}>{value}</option>)}</select>
      <select value={status} onChange={(event) => { setStatus(event.target.value); setCurrentPage(0); }} className="rounded border bg-background p-2"><option value="">All statuses</option>{['PENDING', 'PROCESSING', 'ON_THE_WAY', 'DELIVERED', 'RETURNED', 'RTO', 'CANCELLED'].map((value) => <option key={value}>{value}</option>)}</select>
    </div>
    {loading && <p className="mt-4">Loading orders...</p>}
    {error && <p className="mt-4 text-destructive">{error}</p>}
    {!loading && !error && <><div className="mt-4 overflow-x-auto rounded border bg-card"><table className="w-full text-left text-sm"><thead className="bg-muted"><tr><th className="p-3">Order ID</th><th>Platform</th><th>Date</th><th>Status</th><th>Payment</th></tr></thead><tbody>{orders.content.map((order) => <tr key={order.id} className="border-t"><td className="p-3"><Link className="underline" href={`/orders/${order.id}`}>{order.orderId}</Link></td><td>{order.platform}</td><td>{order.orderDate}</td><td>{order.orderStatus}</td><td>{order.paymentStatus}</td></tr>)}</tbody></table></div>{orders.content.length === 0 && <p className="mt-4 text-muted-foreground">No orders found.</p>}<div className="mt-4 flex items-center gap-3"><button disabled={currentPage === 0} onClick={() => setCurrentPage((page) => page - 1)} className="rounded border px-3 py-2 disabled:opacity-50">Previous</button><span className="text-sm">Page {orders.totalPages === 0 ? 0 : currentPage + 1} of {orders.totalPages}</span><button disabled={currentPage + 1 >= orders.totalPages} onClick={() => setCurrentPage((page) => page + 1)} className="rounded border px-3 py-2 disabled:opacity-50">Next</button></div></>}
  </section></AppShell>;
}
