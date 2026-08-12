"use client";

import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";

type Supplier = { id: number; name: string; supplierId: string; active: boolean };
type SupplierPage = { content: Supplier[] };
type VariantOption = {
  id: number;
  sku: string;
  colorName: string;
  sizeName: string;
  costPrice: number | string;
  sellingPrice: number | string;
  mrp: number | string;
  active: boolean;
  productName: string;
};
type Product = { id: number; productName: string; variants: VariantOption[] };

type PurchaseItem = { variantId: string; quantity: string; unitCost: string; tax: string };

const today = new Date().toISOString().slice(0, 10);

export default function NewPurchasePage() {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [items, setItems] = useState<PurchaseItem[]>([{ variantId: "", quantity: "1", unitCost: "", tax: "0" }]);
  const [form, setForm] = useState({
    supplierId: "",
    purchaseId: "",
    purchaseDate: today,
    invoiceNumber: "",
    invoiceDate: today,
    otherCharges: "0",
    paidAmount: "0",
    paymentStatus: "PENDING",
    remarks: "",
  });

  useEffect(() => {
    api.get<SupplierPage>("/suppliers?active=true&size=200").then((response) => setSuppliers(response.data.content)).catch(() => setError("Unable to load suppliers."));
    api.get<Product[]>("/products").then((response) => setProducts(response.data)).catch(() => setError("Unable to load products and variants."));
  }, []);

  const variants = useMemo(() => products.flatMap((product) => product.variants.filter((variant) => variant.active).map((variant) => ({
    ...variant,
    productName: product.productName,
  }))), [products]);

  const setItem = (index: number, patch: Partial<PurchaseItem>) => {
    setItems((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item));
  };

  const addItem = () => setItems((current) => [...current, { variantId: "", quantity: "1", unitCost: "", tax: "0" }]);
  const removeItem = (index: number) => setItems((current) => current.filter((_, itemIndex) => itemIndex !== index));

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!form.supplierId || !form.purchaseId || !form.invoiceNumber || !items.length) {
      setError("Please complete all required fields.");
      return;
    }
    if (items.some((item) => !item.variantId || Number(item.quantity) <= 0 || Number(item.unitCost) < 0 || Number(item.tax) < 0)) {
      setError("Each purchase item needs a valid variant, quantity, cost, and tax.");
      return;
    }
    setSaving(true);
    try {
      await api.post("/purchases", {
        supplierId: Number(form.supplierId),
        purchaseId: form.purchaseId,
        purchaseDate: form.purchaseDate,
        invoiceNumber: form.invoiceNumber,
        invoiceDate: form.invoiceDate,
        otherCharges: Number(form.otherCharges),
        paidAmount: Number(form.paidAmount),
        paymentStatus: form.paymentStatus,
        remarks: form.remarks,
        items: items.map((item) => ({
          variantId: Number(item.variantId),
          quantity: Number(item.quantity),
          unitCost: Number(item.unitCost),
          tax: Number(item.tax),
        })),
      });
      window.location.href = "/purchases";
    } catch {
      setError("Unable to save purchase. Please check supplier, variant, and amount details.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Procurement</p>
            <h1 className="mt-1 text-2xl font-semibold">Create purchase</h1>
          </div>
          <Link href="/purchases" className="text-sm underline">Back to purchases</Link>
        </div>

        <form onSubmit={submit} className="mt-6 space-y-6 rounded-lg border bg-card p-6 shadow-sm">
          {error && <p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
          <div className="grid gap-4 md:grid-cols-3">
            <Select label="Supplier" value={form.supplierId} onChange={(value) => setForm({ ...form, supplierId: value })} options={suppliers.map((supplier) => ({ value: String(supplier.id), label: `${supplier.name} (${supplier.supplierId})` }))} />
            <Field label="Purchase ID" value={form.purchaseId} onChange={(value) => setForm({ ...form, purchaseId: value })} />
            <Field label="Invoice number" value={form.invoiceNumber} onChange={(value) => setForm({ ...form, invoiceNumber: value })} />
            <Field type="date" label="Purchase date" value={form.purchaseDate} onChange={(value) => setForm({ ...form, purchaseDate: value })} />
            <Field type="date" label="Invoice date" value={form.invoiceDate} onChange={(value) => setForm({ ...form, invoiceDate: value })} />
            <Select label="Payment status" value={form.paymentStatus} onChange={(value) => setForm({ ...form, paymentStatus: value })} options={["PENDING", "PARTIALLY_PAID", "PAID"].map((value) => ({ value, label: value }))} />
            <Field type="number" label="Other charges" value={form.otherCharges} onChange={(value) => setForm({ ...form, otherCharges: value })} />
            <Field type="number" label="Paid amount" value={form.paidAmount} onChange={(value) => setForm({ ...form, paidAmount: value })} />
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Items</h2>
              <button type="button" onClick={addItem} className="rounded border px-3 py-2 text-sm">Add item</button>
            </div>
            {items.map((item, index) => (
              <div key={index} className="grid gap-3 rounded border p-4 md:grid-cols-5">
                <Select
                  label="Variant"
                  value={item.variantId}
                  onChange={(value) => {
                    const variant = variants.find((candidate) => String(candidate.id) === value);
                    setItem(index, { variantId: value, unitCost: variant ? String(variant.costPrice) : item.unitCost });
                  }}
                  options={variants.map((variant) => ({ value: String(variant.id), label: `${variant.productName} / ${variant.colorName} / ${variant.sizeName} / ${variant.sku}` }))}
                />
                <Field type="number" label="Quantity" value={item.quantity} onChange={(value) => setItem(index, { quantity: value })} />
                <Field type="number" label="Unit cost" value={item.unitCost} onChange={(value) => setItem(index, { unitCost: value })} />
                <Field type="number" label="Tax" value={item.tax} onChange={(value) => setItem(index, { tax: value })} />
                <div className="flex items-end">
                  <button type="button" onClick={() => removeItem(index)} disabled={items.length === 1} className="rounded border px-3 py-2 text-sm disabled:opacity-50">
                    Remove
                  </button>
                </div>
              </div>
            ))}
          </div>

          <label className="block text-sm font-medium">
            Remarks
            <textarea value={form.remarks} onChange={(event) => setForm({ ...form, remarks: event.target.value })} className="mt-1 min-h-24 w-full rounded border bg-background px-3 py-2" />
          </label>

          <div className="flex justify-end">
            <button disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-white disabled:opacity-50">
              {saving ? "Saving..." : "Create purchase"}
            </button>
          </div>
        </form>
      </section>
    </AppShell>
  );
}

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <input type={type} required value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 block w-full rounded border bg-background px-3 py-2 font-normal" />
    </label>
  );
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }> }) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 block w-full rounded border bg-background px-3 py-2 font-normal">
        <option value="">Select</option>
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
    </label>
  );
}
