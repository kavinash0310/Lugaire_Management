"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Item = { variantId: number; sku: string; quantity: number; sellingPrice: number };
type Detail = {
  orderDate: string; customerName?: string; customerPhone?: string; shippingAddress?: string; city?: string; state?: string; pincode?: string;
  commission: number; shippingCharge: number; otherCharges: number; items: Item[];
};

export default function EditOrderPage({ params }: { params: Promise<{ id: string }> }) {
  const [id, setId] = useState("");
  const [data, setData] = useState<Detail>();
  const [message, setMessage] = useState("");

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

  function setItem(index: number, field: keyof Item, value: string) {
    setData((current) => current && {
      ...current,
      items: current.items.map((item, itemIndex) => itemIndex === index
        ? { ...item, [field]: field === "sku" ? value : Number(value) }
        : item),
    });
  }

  function addItem() {
    setData((current) => current && {
      ...current,
      items: [...current.items, { variantId: 0, sku: "New item", quantity: 1, sellingPrice: 0 }],
    });
  }

  function removeItem(index: number) {
    setData((current) => current && { ...current, items: current.items.filter((_, itemIndex) => itemIndex !== index) });
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (!data) return;
    setMessage("");
    try {
      await api.put(`/orders/${id}`, {
        ...data,
        items: data.items.map(({ variantId, quantity, sellingPrice }) => ({ variantId, quantity, sellingPrice })),
      });
      setMessage("Order updated and stock reconciled.");
    } catch {
      setMessage("Unable to update the order. Check stock and values.");
    }
  }

  return <AppShell><section className="mx-auto max-w-3xl p-6">
    <h1 className="text-2xl font-semibold">Edit order</h1>
    {!data ? <p className="mt-4">{message || "Loading order..."}</p> : <form onSubmit={save} className="mt-6 space-y-4 rounded border bg-card p-5">
      <input required type="date" value={data.orderDate} onChange={(event) => setData({ ...data, orderDate: event.target.value })} className="rounded border p-2" />
      <input placeholder="Customer name" value={data.customerName || ""} onChange={(event) => setData({ ...data, customerName: event.target.value })} className="w-full rounded border p-2" />
      {data.items.map((item, index) => <div className="grid grid-cols-5 gap-2" key={`${item.variantId}-${index}`}>
        <input required min="1" type="number" aria-label="Variant ID" value={item.variantId || ""} onChange={(event) => setItem(index, "variantId", event.target.value)} className="rounded border p-2" />
        <input readOnly value={item.sku} aria-label="SKU" className="rounded border bg-muted p-2" />
        <input required min="1" type="number" value={item.quantity} onChange={(event) => setItem(index, "quantity", event.target.value)} className="rounded border p-2" />
        <input required min="0" type="number" value={item.sellingPrice} onChange={(event) => setItem(index, "sellingPrice", event.target.value)} className="rounded border p-2" />
        <button type="button" disabled={data.items.length === 1} onClick={() => removeItem(index)} className="rounded border px-2 disabled:opacity-50">Remove</button>
      </div>)}
      <button type="button" onClick={addItem} className="rounded border px-3 py-2">Add item</button>
      <button className="rounded bg-slate-900 px-4 py-2 text-white">Save changes</button>
      <Link className="ml-4 text-sm underline" href={`/orders/${id}`}>Back to order</Link>
      {message && <p>{message}</p>}
    </form>}
  </section></AppShell>;
}
