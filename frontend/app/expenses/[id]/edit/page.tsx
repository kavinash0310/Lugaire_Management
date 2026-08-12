"use client";

import Link from "next/link";
import { use, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { ExpenseForm, type ExpenseRequestPayload } from "@/components/expense-form";
import { api } from "@/lib/api";

type Category = { id: number; name: string; code: string; active: boolean };
type SupplierPage = { content: Array<{ id: number; supplierId: string; name: string; active: boolean }> };

type Expense = {
  expenseDate: string;
  categoryId: number;
  supplierId?: number | null;
  description: string;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  referenceNumber?: string;
  remarks?: string;
};

export default function EditExpensePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [categories, setCategories] = useState<Category[]>([]);
  const [suppliers, setSuppliers] = useState<SupplierPage["content"]>([]);
  const [initial, setInitial] = useState<Expense | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    void Promise.all([
      api.get<Category[]>("/expense-categories").then((response) => setCategories(response.data)),
      api.get<SupplierPage>("/suppliers?size=500").then((response) => setSuppliers(response.data.content)),
      api.get<Expense>(`/expenses/${id}`).then((response) => setInitial(response.data)),
    ]).catch(() => setError("Unable to load expense."));
  }, [id]);

  if (error) {
    return <AppShell><section className="mx-auto max-w-7xl p-5 md:p-8"><p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p></section></AppShell>;
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold">Edit expense</h1>
          </div>
          <Link href={`/expenses/${id}`} className="text-sm underline">Back to expense</Link>
        </div>

        {!initial ? <p className="mt-6 text-sm text-muted-foreground">Loading expense...</p> : (
          <ExpenseForm
            initialValues={{
              expenseDate: initial.expenseDate,
              categoryId: String(initial.categoryId),
              supplierId: initial.supplierId ? String(initial.supplierId) : "",
              description: initial.description,
              amount: String(initial.amount),
              taxAmount: String(initial.taxAmount),
              totalAmount: String(initial.totalAmount),
              paymentMethod: initial.paymentMethod,
              paymentStatus: initial.paymentStatus,
              referenceNumber: initial.referenceNumber ?? "",
              remarks: initial.remarks ?? "",
            }}
            categories={categories}
            suppliers={suppliers}
            submitLabel="Update expense"
            onSubmit={async (payload: ExpenseRequestPayload) => {
              await api.put(`/expenses/${id}`, payload);
              window.location.href = `/expenses/${id}`;
            }}
          />
        )}
      </section>
    </AppShell>
  );
}
