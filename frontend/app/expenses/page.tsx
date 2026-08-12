"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Expense = {
  id: number;
  expenseNumber: string;
  expenseDate: string;
  categoryId: number;
  categoryName: string;
  supplierId?: number | null;
  supplierName?: string | null;
  description: string;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  referenceNumber?: string;
  remarks?: string;
};

type Page = { content: Expense[]; page: number; size: number; totalElements: number; totalPages: number };
type Category = { id: number; name: string; code: string; active: boolean };
type SupplierPage = { content: Array<{ id: number; supplierId: string; name: string; active: boolean }> };

const toCurrency = (value: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(value || 0);

export default function ExpensesPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [supplierId, setSupplierId] = useState("");
  const [paymentStatus, setPaymentStatus] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [minAmount, setMinAmount] = useState("");
  const [maxAmount, setMaxAmount] = useState("");
  const [data, setData] = useState<Page>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [categories, setCategories] = useState<Category[]>([]);
  const [suppliers, setSuppliers] = useState<SupplierPage["content"]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const query = useMemo(() => {
    const params = new URLSearchParams({ search, page: String(page), size: "20" });
    if (categoryId) params.set("categoryId", categoryId);
    if (supplierId) params.set("supplierId", supplierId);
    if (paymentStatus) params.set("paymentStatus", paymentStatus);
    if (paymentMethod) params.set("paymentMethod", paymentMethod);
    if (fromDate) params.set("fromDate", fromDate);
    if (toDate) params.set("toDate", toDate);
    if (minAmount) params.set("minAmount", minAmount);
    if (maxAmount) params.set("maxAmount", maxAmount);
    return params.toString();
  }, [search, page, categoryId, supplierId, paymentStatus, paymentMethod, fromDate, toDate, minAmount, maxAmount]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [expensesResponse, categoriesResponse, suppliersResponse] = await Promise.all([
        api.get<Page>(`/expenses?${query}`),
        api.get<Category[]>("/expense-categories"),
        api.get<SupplierPage>("/suppliers?size=500"),
      ]);
      setData(expensesResponse.data);
      setCategories(categoriesResponse.data);
      setSuppliers(suppliersResponse.data.content);
    } catch {
      setError("Unable to load expenses.");
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    void load();
  }, [load]);

  const markPaid = async (id: number) => {
    try {
      await api.patch(`/expenses/${id}/payment-status`, { paymentStatus: "PAID" });
      await load();
    } catch {
      setError("Unable to update payment status.");
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Expenses</h1>
          </div>
          <Link href="/expenses/new" className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white">Create expense</Link>
        </div>

        <div className="mt-6 grid gap-3 rounded-lg border bg-card p-4 md:grid-cols-3 xl:grid-cols-6">
          <input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} placeholder="Search number or description" className="rounded border px-3 py-2" />
          <select value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All categories</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
          <select value={supplierId} onChange={(event) => { setSupplierId(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All suppliers</option>
            {suppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}
          </select>
          <select value={paymentStatus} onChange={(event) => { setPaymentStatus(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All payment status</option>
            {["PENDING", "PAID"].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
          <select value={paymentMethod} onChange={(event) => { setPaymentMethod(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All payment methods</option>
            {["CASH", "BANK_TRANSFER", "UPI", "CARD", "OTHER"].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
          <input type="date" value={fromDate} onChange={(event) => { setFromDate(event.target.value); setPage(0); }} className="rounded border px-3 py-2" />
          <input type="date" value={toDate} onChange={(event) => { setToDate(event.target.value); setPage(0); }} className="rounded border px-3 py-2" />
          <input type="number" value={minAmount} onChange={(event) => { setMinAmount(event.target.value); setPage(0); }} placeholder="Min amount" className="rounded border px-3 py-2" />
          <input type="number" value={maxAmount} onChange={(event) => { setMaxAmount(event.target.value); setPage(0); }} placeholder="Max amount" className="rounded border px-3 py-2" />
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {loading ? <p className="mt-6 text-sm text-muted-foreground">Loading expenses...</p> : data.content.length === 0 ? <p className="mt-6 rounded border bg-card p-6 text-sm text-muted-foreground">No expenses found.</p> : (
          <div className="mt-6 overflow-x-auto rounded-lg border bg-card shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">Expense Number</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Description</th>
                  <th className="px-4 py-3">Supplier</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Tax</th>
                  <th className="px-4 py-3">Total</th>
                  <th className="px-4 py-3">Payment Method</th>
                  <th className="px-4 py-3">Payment Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((expense) => (
                  <tr key={expense.id} className="border-t">
                    <td className="px-4 py-3 font-medium"><Link href={`/expenses/${expense.id}`} className="underline">{expense.expenseNumber}</Link></td>
                    <td className="px-4 py-3">{expense.expenseDate}</td>
                    <td className="px-4 py-3">{expense.categoryName}</td>
                    <td className="px-4 py-3">{expense.description}</td>
                    <td className="px-4 py-3">{expense.supplierName ?? "-"}</td>
                    <td className="px-4 py-3">{toCurrency(expense.amount)}</td>
                    <td className="px-4 py-3">{toCurrency(expense.taxAmount)}</td>
                    <td className="px-4 py-3">{toCurrency(expense.totalAmount)}</td>
                    <td className="px-4 py-3">{expense.paymentMethod}</td>
                    <td className="px-4 py-3">{expense.paymentStatus}</td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-3">
                        <Link href={`/expenses/${expense.id}/edit`} className="text-slate-700 hover:underline">Edit</Link>
                        <button onClick={() => void markPaid(expense.id)} disabled={expense.paymentStatus === "PAID"} className="text-slate-700 hover:underline disabled:opacity-50">Mark paid</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="mt-4 flex items-center gap-3">
          <button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded border px-3 py-2 disabled:opacity-50">Previous</button>
          <span className="text-sm">Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
          <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((current) => current + 1)} className="rounded border px-3 py-2 disabled:opacity-50">Next</button>
        </div>
      </section>
    </AppShell>
  );
}
