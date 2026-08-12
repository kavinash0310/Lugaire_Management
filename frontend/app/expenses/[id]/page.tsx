"use client";

import Link from "next/link";
import { use, useEffect, useState } from "react";
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
  createdAt: string;
  updatedAt: string;
};

const toCurrency = (value: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(value || 0);

export default function ExpenseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [expense, setExpense] = useState<Expense | null>(null);
  const [paymentStatus, setPaymentStatus] = useState("PENDING");
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<Expense>(`/expenses/${id}`).then((response) => {
      setExpense(response.data);
      setPaymentStatus(response.data.paymentStatus);
    }).catch(() => setError("Unable to load expense."));
  }, [id]);

  const updatePaymentStatus = async () => {
    if (!expense) return;
    try {
      const response = await api.patch<Expense>(`/expenses/${expense.id}/payment-status`, { paymentStatus });
      setExpense(response.data);
      setPaymentStatus(response.data.paymentStatus);
    } catch {
      setError("Unable to update payment status.");
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold">{expense?.expenseNumber ?? "Expense"}</h1>
          </div>
          <div className="flex gap-3">
            <Link href="/expenses" className="text-sm underline">Back</Link>
            <Link href={`/expenses/${id}/edit`} className="text-sm underline">Edit</Link>
          </div>
        </div>

        {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {!expense ? <p className="mt-6 text-sm text-muted-foreground">Loading expense...</p> : (
          <div className="mt-6 space-y-6">
            <div className="grid gap-4 md:grid-cols-3">
              <Card label="Expense date" value={expense.expenseDate} />
              <Card label="Category" value={expense.categoryName} />
              <Card label="Supplier" value={expense.supplierName ?? "-"} />
              <Card label="Amount" value={toCurrency(expense.amount)} />
              <Card label="Tax" value={toCurrency(expense.taxAmount)} />
              <Card label="Total" value={toCurrency(expense.totalAmount)} />
              <Card label="Payment method" value={expense.paymentMethod} />
              <Card label="Payment status" value={expense.paymentStatus} />
              <Card label="Reference" value={expense.referenceNumber ?? "-"} />
            </div>

            <div className="rounded-lg border bg-card p-5 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold">Payment status</h2>
                  <p className="text-sm text-muted-foreground">Update the expense payment status when the bill is actually paid.</p>
                </div>
                <div className="flex items-center gap-2">
                  <select value={paymentStatus} onChange={(event) => setPaymentStatus(event.target.value)} className="rounded border bg-background px-3 py-2 text-sm">
                    {["PENDING", "PAID"].map((value) => <option key={value} value={value}>{value}</option>)}
                  </select>
                  <button onClick={() => void updatePaymentStatus()} className="rounded bg-slate-900 px-3 py-2 text-sm text-white">Update status</button>
                </div>
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <Card label="Remarks" value={expense.remarks ?? "-"} />
              <Card label="Created at" value={expense.createdAt} />
              <Card label="Updated at" value={expense.updatedAt} />
              <Card label="Description" value={expense.description} />
            </div>
          </div>
        )}
      </section>
    </AppShell>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-card p-4 shadow-sm">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 text-lg font-semibold">{value}</p>
    </div>
  );
}
