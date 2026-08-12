"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { ExpenseForm, type ExpenseRequestPayload } from "@/components/expense-form";
import { api } from "@/lib/api";

type Category = { id: number; name: string; code: string; active: boolean };
type SupplierPage = { content: Array<{ id: number; supplierId: string; name: string; active: boolean }> };

export default function NewExpensePage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [suppliers, setSuppliers] = useState<SupplierPage["content"]>([]);

  useEffect(() => {
    void Promise.all([
      api.get<Category[]>("/expense-categories").then((response) => setCategories(response.data)),
      api.get<SupplierPage>("/suppliers?size=500").then((response) => setSuppliers(response.data.content)),
    ]).catch(() => {
      setCategories([]);
      setSuppliers([]);
    });
  }, []);

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold">Create expense</h1>
          </div>
          <Link href="/expenses" className="text-sm underline">Back to expenses</Link>
        </div>

        <ExpenseForm
          categories={categories}
          suppliers={suppliers}
          submitLabel="Create expense"
          onSubmit={async (payload: ExpenseRequestPayload) => {
            await api.post("/expenses", payload);
            window.location.href = "/expenses";
          }}
        />
      </section>
    </AppShell>
  );
}
