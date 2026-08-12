"use client";

import { FormEvent, useMemo, useState } from "react";

type CategoryOption = { id: number; name: string; code: string; active: boolean };
type SupplierOption = { id: number; supplierId: string; name: string; active: boolean };

type ExpenseFormValues = {
  expenseDate: string;
  categoryId: string;
  supplierId: string;
  description: string;
  amount: string;
  taxAmount: string;
  totalAmount: string;
  paymentMethod: string;
  paymentStatus: string;
  referenceNumber: string;
  remarks: string;
};

export type ExpenseRequestPayload = {
  expenseDate: string;
  categoryId: number;
  supplierId: number | null;
  description: string;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  paymentMethod: "CASH" | "BANK_TRANSFER" | "UPI" | "CARD" | "OTHER";
  paymentStatus: "PENDING" | "PAID";
  referenceNumber: string;
  remarks: string;
};

const today = new Date().toISOString().slice(0, 10);

export function ExpenseForm({
  initialValues,
  categories,
  suppliers,
  onSubmit,
  submitLabel,
}: {
  initialValues?: Partial<ExpenseFormValues>;
  categories: CategoryOption[];
  suppliers: SupplierOption[];
  onSubmit: (payload: ExpenseRequestPayload) => Promise<void>;
  submitLabel: string;
}) {
  const [form, setForm] = useState<ExpenseFormValues>({
    expenseDate: today,
    categoryId: "",
    supplierId: "",
    description: "",
    amount: "0",
    taxAmount: "0",
    totalAmount: "0",
    paymentMethod: "CASH",
    paymentStatus: "PENDING",
    referenceNumber: "",
    remarks: "",
    ...initialValues,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const total = useMemo(() => Number(form.amount || 0) + Number(form.taxAmount || 0), [form.amount, form.taxAmount]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!form.categoryId) {
      setError("Please select an expense category.");
      return;
    }
    if (!form.description.trim()) {
      setError("Description is required.");
      return;
    }
    setSaving(true);
    try {
      await onSubmit({
        expenseDate: form.expenseDate,
        categoryId: Number(form.categoryId),
        supplierId: form.supplierId ? Number(form.supplierId) : null,
        description: form.description.trim(),
        amount: Number(form.amount || 0),
        taxAmount: Number(form.taxAmount || 0),
        totalAmount: total,
        paymentMethod: form.paymentMethod as ExpenseRequestPayload["paymentMethod"],
        paymentStatus: form.paymentStatus as ExpenseRequestPayload["paymentStatus"],
        referenceNumber: form.referenceNumber.trim(),
        remarks: form.remarks,
      });
    } catch {
      setError("Unable to save expense. Please review the values and try again.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={submit} className="mt-6 space-y-6 rounded-lg border bg-card p-6 shadow-sm">
      {error && <p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <div className="grid gap-4 md:grid-cols-3">
        <Field type="date" label="Expense date" value={form.expenseDate} onChange={(value) => setForm((current) => ({ ...current, expenseDate: value }))} />
        <Select label="Category" value={form.categoryId} onChange={(value) => setForm((current) => ({ ...current, categoryId: value }))} options={categories.map((category) => ({ value: String(category.id), label: `${category.name} (${category.code})` }))} />
        <Select label="Supplier" value={form.supplierId} onChange={(value) => setForm((current) => ({ ...current, supplierId: value }))} options={suppliers.map((supplier) => ({ value: String(supplier.id), label: `${supplier.name} (${supplier.supplierId})` }))} allowEmpty />
        <label className="block text-sm font-medium md:col-span-2">
          Description
          <input value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} className="mt-1 block w-full rounded border bg-background px-3 py-2 font-normal" />
        </label>
        <Field label="Reference number" value={form.referenceNumber} onChange={(value) => setForm((current) => ({ ...current, referenceNumber: value }))} />
        <Field type="number" label="Amount" value={form.amount} onChange={(value) => setForm((current) => ({ ...current, amount: value }))} />
        <Field type="number" label="Tax" value={form.taxAmount} onChange={(value) => setForm((current) => ({ ...current, taxAmount: value }))} />
        <Field readOnly type="number" label="Total" value={total.toFixed(2)} onChange={(value) => setForm((current) => ({ ...current, totalAmount: value }))} />
        <Select label="Payment method" value={form.paymentMethod} onChange={(value) => setForm((current) => ({ ...current, paymentMethod: value }))} options={["CASH", "BANK_TRANSFER", "UPI", "CARD", "OTHER"].map((value) => ({ value, label: value }))} />
        <Select label="Payment status" value={form.paymentStatus} onChange={(value) => setForm((current) => ({ ...current, paymentStatus: value }))} options={["PENDING", "PAID"].map((value) => ({ value, label: value }))} />
      </div>

      <label className="block text-sm font-medium">
        Remarks
        <textarea value={form.remarks} onChange={(event) => setForm((current) => ({ ...current, remarks: event.target.value }))} className="mt-1 min-h-24 w-full rounded border bg-background px-3 py-2" />
      </label>

      <div className="rounded border bg-muted/20 p-4 text-sm">
        <p><strong>Amount:</strong> ₹{Number(form.amount || 0).toFixed(2)}</p>
        <p><strong>Tax:</strong> ₹{Number(form.taxAmount || 0).toFixed(2)}</p>
        <p><strong>Total:</strong> ₹{total.toFixed(2)}</p>
      </div>

      <div className="flex justify-end">
        <button disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-white disabled:opacity-50">{saving ? "Saving..." : submitLabel}</button>
      </div>
    </form>
  );
}

function Field({ label, value, onChange, type = "text", readOnly = false }: { label: string; value: string; onChange: (value: string) => void; type?: string; readOnly?: boolean }) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <input type={type} required readOnly={readOnly} value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 block w-full rounded border bg-background px-3 py-2 font-normal disabled:opacity-50" />
    </label>
  );
}

function Select({ label, value, onChange, options, allowEmpty = false }: { label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }>; allowEmpty?: boolean }) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 block w-full rounded border bg-background px-3 py-2 font-normal">
        {allowEmpty && <option value="">None</option>}
        {!allowEmpty && <option value="">Select</option>}
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
    </label>
  );
}
