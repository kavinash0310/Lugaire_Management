"use client";

import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

type Supplier = { id: number; name: string; supplierId: string; active: boolean };
type SupplierPage = { content: Supplier[] };
type VariantOption = {
  id: number;
  sku: string;
  colorName: string;
  sizeName: string;
  costPrice: number | string;
  active: boolean;
  productName: string;
};
type Product = { id: number; productName: string; variants: VariantOption[] };

type PurchaseItem = {
  id: number;
  variantId: number;
  productName: string;
  colorName: string;
  sizeName: string;
  skuSnapshot: string;
  quantity: number;
  unitCost: number | string;
  tax: number | string;
  lineTotal: number | string;
};

type Purchase = {
  id: number;
  purchaseId: string;
  supplierId: number;
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
  remarks?: string | null;
  items: PurchaseItem[];
};

const paymentStatuses = ["PENDING", "PARTIALLY_PAID", "PAID"];

export default function EditPurchasePage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = params.id;
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({
    supplierId: "",
    purchaseId: "",
    purchaseDate: "",
    invoiceNumber: "",
    invoiceDate: "",
    otherCharges: "0",
    paidAmount: "0",
    paymentStatus: "PENDING",
    remarks: "",
  });
  const [items, setItems] = useState<Array<{ variantId: string; quantity: string; unitCost: string; tax: string }>>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [purchaseResponse, supplierResponse, productResponse] = await Promise.all([
        api.get<Purchase>(`/purchases/${id}`),
        api.get<SupplierPage>("/suppliers?active=true&size=200"),
        api.get<Product[]>("/products"),
      ]);
      const currentPurchase = purchaseResponse.data;
      setSuppliers(supplierResponse.data.content);
      setProducts(productResponse.data);
      setForm({
        supplierId: String(currentPurchase.supplierId),
        purchaseId: currentPurchase.purchaseId,
        purchaseDate: currentPurchase.purchaseDate,
        invoiceNumber: currentPurchase.invoiceNumber,
        invoiceDate: currentPurchase.invoiceDate,
        otherCharges: String(currentPurchase.otherCharges),
        paidAmount: String(currentPurchase.paidAmount),
        paymentStatus: currentPurchase.paymentStatus,
        remarks: currentPurchase.remarks ?? "",
      });
      setItems(currentPurchase.items.map((item) => ({
        variantId: String(item.variantId),
        quantity: String(item.quantity),
        unitCost: String(item.unitCost),
        tax: String(item.tax),
      })));
    } catch {
      setError("Unable to load purchase data.");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const variants = useMemo(
    () =>
      products.flatMap((product) =>
        product.variants
          .filter((variant) => variant.active)
          .map((variant) => ({ ...variant, productName: product.productName })),
      ),
    [products],
  );

  const setItem = (index: number, patch: Partial<{ variantId: string; quantity: string; unitCost: string; tax: string }>) => {
    setItems((current) => current.map((item, itemIndex) => (itemIndex === index ? { ...item, ...patch } : item)));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!form.supplierId || !form.purchaseId || !form.invoiceNumber || !items.length) {
      setError("Please complete all required fields.");
      return;
    }
    if (items.some((item) => !item.variantId || Number(item.quantity) <= 0 || Number(item.unitCost) < 0 || Number(item.tax) < 0)) {
      setError("Each item needs a valid variant, quantity, cost, and tax.");
      return;
    }
    setSaving(true);
    try {
      await api.put(`/purchases/${id}`, {
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
      router.push(`/purchases/${id}`);
      router.refresh();
    } catch {
      setError("Unable to update purchase. Check the supplier, variants, and amounts.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <AppShell>
        <section className="mx-auto max-w-6xl p-5 md:p-8">
          <p>Loading purchase...</p>
        </section>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Procurement</p>
            <h1 className="mt-1 text-2xl font-semibold">Edit purchase</h1>
          </div>
          <Link href={`/purchases/${id}`} className="text-sm underline">
            Back to details
          </Link>
        </div>

        <form onSubmit={submit} className="mt-6 space-y-6 rounded-lg border bg-card p-6 shadow-sm">
          {error && <p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
          <div className="grid gap-4 md:grid-cols-3">
            <Select label="Supplier" value={form.supplierId} onChange={(value) => setForm({ ...form, supplierId: value })} options={suppliers.map((supplier) => ({ value: String(supplier.id), label: `${supplier.name} (${supplier.supplierId})` }))} />
            <Field label="Purchase ID" value={form.purchaseId} onChange={(value) => setForm({ ...form, purchaseId: value })} />
            <Field label="Invoice number" value={form.invoiceNumber} onChange={(value) => setForm({ ...form, invoiceNumber: value })} />
            <Field type="date" label="Purchase date" value={form.purchaseDate} onChange={(value) => setForm({ ...form, purchaseDate: value })} />
            <Field type="date" label="Invoice date" value={form.invoiceDate} onChange={(value) => setForm({ ...form, invoiceDate: value })} />
            <Select label="Payment status" value={form.paymentStatus} onChange={(value) => setForm({ ...form, paymentStatus: value })} options={paymentStatuses.map((value) => ({ value, label: value }))} />
            <Field type="number" label="Other charges" value={form.otherCharges} onChange={(value) => setForm({ ...form, otherCharges: value })} />
            <Field type="number" label="Paid amount" value={form.paidAmount} onChange={(value) => setForm({ ...form, paidAmount: value })} />
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Items</h2>
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
              </div>
            ))}
          </div>

          <label className="block text-sm font-medium">
            Remarks
            <textarea value={form.remarks} onChange={(event) => setForm({ ...form, remarks: event.target.value })} className="mt-1 min-h-24 w-full rounded border bg-background px-3 py-2" />
          </label>

          <div className="flex justify-end gap-3">
            <Link href={`/purchases/${id}`} className="rounded border px-4 py-2 text-sm">
              Cancel
            </Link>
            <button disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-white disabled:opacity-50">
              {saving ? "Saving..." : "Save changes"}
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
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}
