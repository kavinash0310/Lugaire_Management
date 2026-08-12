"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Purchase = {
  id: number;
  purchaseId: string;
  supplierId: number;
  supplierName: string;
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
};

type PurchasePage = {
  content: Purchase[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type Supplier = { id: number; supplierId: string; name: string; active: boolean };
type SupplierPage = { content: Supplier[] };

export default function PurchasesPage() {
  const [data, setData] = useState<PurchasePage>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [search, setSearch] = useState("");
  const [supplierId, setSupplierId] = useState("");
  const [status, setStatus] = useState("");
  const [paymentStatus, setPaymentStatus] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<SupplierPage>("/suppliers?active=true&size=200").then((response) => setSuppliers(response.data.content)).catch(() => undefined);
  }, []);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      const query = new URLSearchParams({ search, page: String(page), size: "20" });
      if (supplierId) query.set("supplierId", supplierId);
      if (status) query.set("status", status);
      if (paymentStatus) query.set("paymentStatus", paymentStatus);
      try {
        const response = await api.get<PurchasePage>(`/purchases?${query.toString()}`);
        setData(response.data);
      } catch {
        setError("Unable to load purchases.");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [search, supplierId, status, paymentStatus, page]);

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Procurement</p>
            <h1 className="mt-1 text-2xl font-semibold">Purchases</h1>
          </div>
          <Link href="/purchases/new" className="rounded bg-slate-900 px-4 py-2 text-white">
            Add purchase
          </Link>
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          <input
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Search purchase ID, invoice or supplier"
            className="min-w-80 rounded border px-3 py-2"
          />
          <select
            value={supplierId}
            onChange={(event) => {
              setSupplierId(event.target.value);
              setPage(0);
            }}
            className="rounded border bg-background px-3 py-2"
          >
            <option value="">All suppliers</option>
            {suppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}
          </select>
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            className="rounded border bg-background px-3 py-2"
          >
            <option value="">All statuses</option>
            {["DRAFT", "RECEIVED", "CANCELLED"].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
          <select
            value={paymentStatus}
            onChange={(event) => {
              setPaymentStatus(event.target.value);
              setPage(0);
            }}
            className="rounded border bg-background px-3 py-2"
          >
            <option value="">All payment statuses</option>
            {["PENDING", "PARTIALLY_PAID", "PAID"].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
        </div>

        {loading && <p className="mt-4">Loading purchases...</p>}
        {error && <p className="mt-4 text-destructive">{error}</p>}

        {!loading && !error && (
          <>
            <div className="mt-4 overflow-x-auto rounded border bg-card">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted">
                  <tr>
                    <th className="p-3">Purchase ID</th>
                    <th>Supplier</th>
                    <th>Date</th>
                    <th>Invoice</th>
                    <th>Status</th>
                    <th>Payment</th>
                    <th>Inventory</th>
                    <th>Total</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((purchase) => (
                    <tr key={purchase.id} className="border-t">
                      <td className="p-3 font-medium">{purchase.purchaseId}</td>
                      <td>{purchase.supplierName}</td>
                      <td>{purchase.purchaseDate}</td>
                      <td>{purchase.invoiceNumber}</td>
                      <td>{purchase.purchaseStatus}</td>
                      <td>{purchase.paymentStatus}</td>
                      <td>{purchase.inventoryReceived ? "Received" : "Pending"}</td>
                      <td>₹{Number(purchase.totalAmount).toFixed(2)}</td>
                      <td className="p-3 text-right">
                        <Link href={`/purchases/${purchase.id}`} className="underline">
                          View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {data.content.length === 0 && <p className="mt-4 text-muted-foreground">No purchases found.</p>}
            <div className="mt-4 flex items-center gap-3">
              <button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded border px-3 py-2 disabled:opacity-50">
                Previous
              </button>
              <span className="text-sm">Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((current) => current + 1)} className="rounded border px-3 py-2 disabled:opacity-50">
                Next
              </button>
            </div>
          </>
        )}
      </section>
    </AppShell>
  );
}
