"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type OrderItem = {
  variantId: number;
  sku: string;
  quantity: number;
  sellingPrice: number;
};

type Detail = {
  orderDate: string;
  customerName?: string;
  customerPhone?: string;
  shippingAddress?: string;
  city?: string;
  state?: string;
  pincode?: string;
  commission: number;
  shippingCharge: number;
  otherCharges: number;
  items: OrderItem[];
};

export default function EditOrderPage({ params }: { params: Promise<{ id: string }> }) {
  const [id, setId] = useState("");
  const [data, setData] = useState<Detail | null>(null);
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    params.then(async ({ id: orderId }) => {
      setId(orderId);
      try {
        const response = await api.get<Detail>(`/orders/${orderId}`);
        setData(response.data);
      } catch {
        setMessage("Unable to load this order.");
      }
    });
  }, [params]);

  const setItem = (index: number, field: keyof OrderItem, value: string) => {
    setData((current) => current && {
      ...current,
      items: current.items.map((item, itemIndex) => itemIndex === index
        ? { ...item, [field]: field === "sku" ? value : Number(value) }
        : item),
    });
  };

  const addItem = () => {
    setData((current) => current && ({
      ...current,
      items: [...current.items, { variantId: 0, sku: "", quantity: 1, sellingPrice: 0 }],
    }));
  };

  const removeItem = (index: number) => {
    setData((current) => current && ({ ...current, items: current.items.filter((_, itemIndex) => itemIndex !== index) }));
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (!data) return;
    setSaving(true);
    setMessage("");
    try {
      await api.put(`/orders/${id}`, {
        orderDate: data.orderDate,
        customerName: data.customerName,
        customerPhone: data.customerPhone,
        shippingAddress: data.shippingAddress,
        city: data.city,
        state: data.state,
        pincode: data.pincode,
        commission: data.commission,
        shippingCharge: data.shippingCharge,
        otherCharges: data.otherCharges,
        items: data.items.map(({ variantId, quantity, sellingPrice }) => ({ variantId, quantity, sellingPrice })),
      });
      setMessage("Order updated and stock reconciled.");
    } catch {
      setMessage("Unable to update the order. Check stock and values.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-3xl p-6">
        <div className="flex items-end justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Sales</p>
            <h1 className="text-2xl font-semibold">Edit order</h1>
          </div>
          <Link className="text-sm underline" href={`/orders/${id}`}>Back to order</Link>
        </div>
        {!data ? (
          <p className="mt-4 text-sm text-muted-foreground">{message || "Loading order..."}</p>
        ) : (
          <form onSubmit={save} className="mt-6 space-y-4 rounded border bg-card p-5">
            <input required type="date" value={data.orderDate} onChange={(event) => setData({ ...data, orderDate: event.target.value })} className="rounded border p-2" />
            <input placeholder="Customer name" value={data.customerName || ""} onChange={(event) => setData({ ...data, customerName: event.target.value })} className="w-full rounded border p-2" />
            <input placeholder="Customer phone" value={data.customerPhone || ""} onChange={(event) => setData({ ...data, customerPhone: event.target.value })} className="w-full rounded border p-2" />
            <input placeholder="Shipping address" value={data.shippingAddress || ""} onChange={(event) => setData({ ...data, shippingAddress: event.target.value })} className="w-full rounded border p-2" />
            <div className="grid gap-3 md:grid-cols-3">
              <input placeholder="City" value={data.city || ""} onChange={(event) => setData({ ...data, city: event.target.value })} className="rounded border p-2" />
              <input placeholder="State" value={data.state || ""} onChange={(event) => setData({ ...data, state: event.target.value })} className="rounded border p-2" />
              <input placeholder="Pincode" value={data.pincode || ""} onChange={(event) => setData({ ...data, pincode: event.target.value })} className="rounded border p-2" />
            </div>
            <div className="grid gap-3 md:grid-cols-3">
              <input required type="number" min="0" value={data.commission} onChange={(event) => setData({ ...data, commission: Number(event.target.value) })} className="rounded border p-2" />
              <input required type="number" min="0" value={data.shippingCharge} onChange={(event) => setData({ ...data, shippingCharge: Number(event.target.value) })} className="rounded border p-2" />
              <input required type="number" min="0" value={data.otherCharges} onChange={(event) => setData({ ...data, otherCharges: Number(event.target.value) })} className="rounded border p-2" />
            </div>
            {data.items.map((item, index) => (
              <div className="grid grid-cols-1 gap-2 rounded border p-3 md:grid-cols-5" key={`${item.variantId}-${index}`}>
                <input required min="1" type="number" aria-label="Variant ID" value={item.variantId || ""} onChange={(event) => setItem(index, "variantId", event.target.value)} className="rounded border p-2" />
                <input readOnly value={item.sku} aria-label="SKU" className="rounded border bg-muted p-2" />
                <input required min="1" type="number" value={item.quantity} onChange={(event) => setItem(index, "quantity", event.target.value)} className="rounded border p-2" />
                <input required min="0" type="number" value={item.sellingPrice} onChange={(event) => setItem(index, "sellingPrice", event.target.value)} className="rounded border p-2" />
                <button type="button" disabled={data.items.length === 1} onClick={() => removeItem(index)} className="rounded border px-2 py-2 disabled:opacity-50">Remove</button>
              </div>
            ))}
            <button type="button" onClick={addItem} className="rounded border px-3 py-2">Add item</button>
            <button disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-white disabled:opacity-50">
              {saving ? "Saving..." : "Save changes"}
            </button>
            {message && <p className="text-sm">{message}</p>}
          </form>
        )}
      </section>
    </AppShell>
  );
}
