"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type ReturnRecord = {
  id: number;
  orderId: string;
  orderPlatform: string;
  productName: string;
  color: string;
  size: string;
  sku: string;
  type: string;
  reason: string;
  quantity: number;
  totalLoss: number;
  resellable: boolean;
  status: string;
  returnDate: string;
};

type ReturnPage = {
  content: ReturnRecord[];
  totalPages: number;
};

export default function ReturnsPage() {
  const [data, setData] = useState<ReturnPage>({ content: [], totalPages: 0 });
  const [search, setSearch] = useState("");
  const [platform, setPlatform] = useState("");
  const [type, setType] = useState("");
  const [status, setStatus] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const query = new URLSearchParams({ search, page: String(page), size: "20" });
    if (platform) query.set("platform", platform);
    if (type) query.set("type", type);
    if (status) query.set("status", status);
    if (fromDate) query.set("fromDate", fromDate);
    if (toDate) query.set("toDate", toDate);

    setLoading(true);
    api.get<ReturnPage>(`/returns?${query.toString()}`)
      .then((response) => {
        setData(response.data);
        setError("");
      })
      .catch(() => setError("Unable to load returns."))
      .finally(() => setLoading(false));
  }, [search, platform, type, status, fromDate, toDate, page]);

  function resetPage(setter: (value: string) => void, value: string) {
    setter(value);
    setPage(0);
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-6">
        <div className="flex justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Returns & RTO</p>
            <h1 className="text-2xl font-semibold">Returns management</h1>
          </div>
          <Link href="/returns/new" className="rounded bg-slate-900 px-4 py-2 text-white">Create return</Link>
        </div>

        <div className="mt-6 grid gap-3 md:grid-cols-3 lg:grid-cols-6">
          <input value={search} onChange={(event) => resetPage(setSearch, event.target.value)} placeholder="Search order or SKU" className="rounded border p-2 lg:col-span-2" />
          <select value={platform} onChange={(event) => resetPage(setPlatform, event.target.value)} className="rounded border bg-background p-2"><option value="">All platforms</option>{["MEESHO", "AMAZON", "FLIPKART", "WEBSITE", "OTHER"].map((value) => <option key={value}>{value}</option>)}</select>
          <select value={type} onChange={(event) => resetPage(setType, event.target.value)} className="rounded border bg-background p-2"><option value="">All types</option><option>RETURNED</option><option>RTO</option></select>
          <select value={status} onChange={(event) => resetPage(setStatus, event.target.value)} className="rounded border bg-background p-2"><option value="">All statuses</option>{["INITIATED", "RECEIVED", "INSPECTED", "COMPLETED"].map((value) => <option key={value}>{value}</option>)}</select>
          <input type="date" value={fromDate} onChange={(event) => resetPage(setFromDate, event.target.value)} className="rounded border p-2" />
          <input type="date" value={toDate} onChange={(event) => resetPage(setToDate, event.target.value)} className="rounded border p-2" />
        </div>

        {loading && <p className="mt-4">Loading returns...</p>}
        {error && <p className="mt-4 text-destructive">{error}</p>}
        {!loading && !error && (
          <>
            <div className="mt-4 overflow-x-auto rounded border">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted">
                  <tr>
                    <th className="p-3">Date</th>
                    <th>Order</th>
                    <th>Product</th>
                    <th>SKU</th>
                    <th>Type</th>
                    <th>Reason</th>
                    <th>Qty</th>
                    <th>Loss</th>
                    <th>Resellable</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((record) => (
                    <tr key={record.id} className="border-t">
                      <td className="p-3">{record.returnDate}</td>
                      <td>{record.orderId}</td>
                      <td>{record.productName} / {record.color} / {record.size}</td>
                      <td>{record.sku}</td>
                      <td>{record.type}</td>
                      <td>{record.reason}</td>
                      <td>{record.quantity}</td>
                      <td>Rs. {record.totalLoss}</td>
                      <td>{record.resellable ? "Yes" : "No"}</td>
                      <td>{record.status}</td>
                      <td><Link className="underline" href={`/returns/${record.id}`}>View</Link></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {data.content.length === 0 && <p className="mt-4 text-muted-foreground">No returns found.</p>}
            <div className="mt-4 flex gap-3">
              <button disabled={page === 0} onClick={() => setPage(page - 1)} className="rounded border px-3 py-2 disabled:opacity-50">Previous</button>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="rounded border px-3 py-2 disabled:opacity-50">Next</button>
            </div>
          </>
        )}
      </section>
    </AppShell>
  );
}
