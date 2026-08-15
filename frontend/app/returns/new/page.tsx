"use client";

import Link from "next/link";
import { type FormEvent, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type OrderItem = {
  id: number;
  productName: string;
  color: string;
  size: string;
  sku: string;
  quantity: number;
  returnedQuantity: number;
  remainingReturnableQuantity: number;
};

type OrderDetail = { items: OrderItem[] };

export default function NewReturnPage() {
  const [orderId, setOrderId] = useState("");
  const [items, setItems] = useState<OrderItem[]>([]);
  const [orderItemId, setOrderItemId] = useState("");
  const [type, setType] = useState("RETURNED");
  const [reason, setReason] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [shippingLoss, setShippingLoss] = useState("0");
  const [otherLoss, setOtherLoss] = useState("0");
  const [resellable, setResellable] = useState(true);
  const [remarks, setRemarks] = useState("");
  const [message, setMessage] = useState("");

  const selectedItem = items.find((item) => String(item.id) === orderItemId);
  const remainingQuantity = selectedItem?.remainingReturnableQuantity ?? 0;
  const totalLoss = useMemo(() => Number(shippingLoss || 0) + Number(otherLoss || 0), [shippingLoss, otherLoss]);

  async function loadOrder() {
    try {
      const response = await api.get<OrderDetail>(`/orders/${orderId.trim()}`);
      setItems(response.data.items);
      setOrderItemId("");
      setMessage("");
    } catch {
      setItems([]);
      setMessage("Order not found.");
    }
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (Number(quantity) > remainingQuantity) {
      setMessage("Return quantity cannot exceed the remaining returnable quantity.");
      return;
    }
    try {
      await api.post("/returns", {
        orderItemId: Number(orderItemId),
        type,
        reason,
        quantity: Number(quantity),
        shippingLoss: Number(shippingLoss),
        otherLoss: Number(otherLoss),
        resellable,
        returnDate: new Date().toISOString().slice(0, 10),
        remarks,
      });
      setMessage("Return record created.");
    } catch {
      setMessage("Unable to create return. Check quantity and values.");
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-2xl p-6">
        <h1 className="text-2xl font-semibold">Create return or RTO</h1>
        <div className="mt-5 flex gap-2">
          <input value={orderId} onChange={(event) => setOrderId(event.target.value)} placeholder="Internal order ID" className="flex-1 rounded border p-2" />
          <button type="button" onClick={loadOrder} className="rounded border px-3">Find order</button>
        </div>
        <form onSubmit={save} className="mt-4 space-y-4 rounded border p-5">
          <select required value={orderItemId} onChange={(event) => setOrderItemId(event.target.value)} className="w-full rounded border p-2">
            <option value="">Select order item</option>
            {items.map((item) => (
              <option key={item.id} value={item.id}>
                {item.productName} / {item.color} / {item.size} / {item.sku} - ordered: {item.quantity}
              </option>
            ))}
          </select>
          {selectedItem && (
            <div className="rounded border bg-muted/30 p-3 text-sm">
              <p>{selectedItem.productName} / {selectedItem.color} / {selectedItem.size}</p>
              <p className="mt-1 text-muted-foreground">SKU: {selectedItem.sku}</p>
              <p className="text-muted-foreground">Ordered quantity: {selectedItem.quantity}</p>
              <p className="text-muted-foreground">Already returned: {selectedItem.returnedQuantity}</p>
              <p className="text-muted-foreground">Remaining returnable quantity: {selectedItem.remainingReturnableQuantity}</p>
            </div>
          )}
          <select value={type} onChange={(event) => setType(event.target.value)} className="w-full rounded border p-2">
            <option>RETURNED</option>
            <option>RTO</option>
          </select>
          <input required value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Reason" className="w-full rounded border p-2" />
          <input required min="1" max={remainingQuantity || undefined} type="number" value={quantity} onChange={(event) => setQuantity(event.target.value)} className="w-full rounded border p-2" />
          <label className="flex gap-2"><input type="checkbox" checked={resellable} onChange={(event) => setResellable(event.target.checked)} />Resellable stock</label>
          <input required min="0" type="number" value={shippingLoss} onChange={(event) => setShippingLoss(event.target.value)} placeholder="Shipping loss" className="w-full rounded border p-2" />
          <input required min="0" type="number" value={otherLoss} onChange={(event) => setOtherLoss(event.target.value)} placeholder="Other loss" className="w-full rounded border p-2" />
          <p className="text-sm font-medium">Loss preview: Rs. {totalLoss.toFixed(2)} plus product cost calculated by backend</p>
          <textarea value={remarks} onChange={(event) => setRemarks(event.target.value)} placeholder="Remarks (optional)" className="w-full rounded border p-2" />
          <div className="flex items-center gap-3">
            <button className="rounded bg-slate-900 px-4 py-2 text-white">Create record</button>
            <Link href="/returns" className="text-sm underline">Back to returns</Link>
          </div>
          {message && <p>{message}</p>}
        </form>
      </section>
    </AppShell>
  );
}
