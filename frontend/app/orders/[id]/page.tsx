"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type OrderDetail = {
  id: number;
  orderId: string;
  platform: string;
  orderDate: string;
  orderStatus: string;
  paymentStatus: string;
  customerName?: string;
  customerPhone?: string;
  shippingAddress?: string;
  city?: string;
  state?: string;
  pincode?: string;
  totalOrderValue: number;
  commission: number;
  shippingCharge: number;
  otherCharges: number;
  netAmount: number;
  items: Array<{ id: number; variantId: number; productName: string; color: string; size: string; sku: string; quantity: number; returnedQuantity: number; remainingReturnableQuantity: number; sellingPrice: number; lineTotal: number }>;
};

export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [id, setId] = useState("");

  useEffect(() => {
    params
      .then(({ id: orderId }) => {
        setId(orderId);
        return api.get<OrderDetail>(`/orders/${orderId}`);
      })
      .then((response) => setOrder(response.data))
      .catch(() => setError("Unable to load this order."));
  }, [params]);

  const updateStatus = async (status: string) => {
    if (!id) return;
    setSaving(true);
    setError("");
    try {
      await api.patch(`/orders/${id}/status`, { status });
      setOrder((current) => current ? { ...current, orderStatus: status } : current);
    } catch {
      setError("Unable to update the order status.");
    } finally {
      setSaving(false);
    }
  };

  const updatePaymentStatus = async (status: string) => {
    if (!id) return;
    setSaving(true);
    setError("");
    try {
      await api.patch(`/orders/${id}/payment-status`, { status });
      setOrder((current) => current ? { ...current, paymentStatus: status } : current);
    } catch {
      setError("Unable to update the payment status.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-6">
        {!order && !error && <p className="text-sm text-muted-foreground">Loading order...</p>}
        {error && <p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {order && (
          <>
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Sales</p>
                <h1 className="text-2xl font-semibold">{order.orderId}</h1>
                <p className="mt-2 text-sm text-muted-foreground">
                  {order.platform} · {order.orderStatus} · {order.paymentStatus}
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <Link className="rounded border px-3 py-2 text-sm" href={`/orders/${id}/edit`}>Edit order</Link>
                <label className="text-sm">
                  Order status
                  <select
                    className="ml-2 rounded border bg-background p-2"
                    disabled={saving}
                    value={order.orderStatus}
                    onChange={(event) => void updateStatus(event.target.value)}
                  >
                    {["PENDING", "PROCESSING", "ON_THE_WAY", "DELIVERED", "CANCELLED", "RTO", "RETURNED"].map((value) => (
                      <option key={value} value={value}>{value}</option>
                    ))}
                  </select>
                </label>
                <label className="text-sm">
                  Payment status
                  <select
                    className="ml-2 rounded border bg-background p-2"
                    disabled={saving}
                    value={order.paymentStatus}
                    onChange={(event) => void updatePaymentStatus(event.target.value)}
                  >
                    {["PENDING", "PAID", "DEDUCTED"].map((value) => (
                      <option key={value} value={value}>{value}</option>
                    ))}
                  </select>
                </label>
              </div>
            </div>

            <div className="mt-6 grid gap-4 md:grid-cols-3">
              <Card label="Customer" value={order.customerName || "—"} />
              <Card label="Phone" value={order.customerPhone || "—"} />
              <Card label="Address" value={[order.shippingAddress, order.city, order.state, order.pincode].filter(Boolean).join(", ") || "—"} />
              <Card label="Total" value={`₹${order.totalOrderValue.toFixed(2)}`} />
              <Card label="Commission" value={`₹${order.commission.toFixed(2)}`} />
              <Card label="Shipping" value={`₹${order.shippingCharge.toFixed(2)}`} />
              <Card label="Other charges" value={`₹${order.otherCharges.toFixed(2)}`} />
              <Card label="Net amount" value={`₹${order.netAmount.toFixed(2)}`} />
            </div>

            <div className="mt-6 overflow-hidden rounded border bg-card">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted">
                  <tr>
                    <th className="p-3">SKU</th>
                    <th className="p-3">Product</th>
                    <th className="p-3">Qty</th>
                    <th className="p-3">Returned</th>
                    <th className="p-3">Remaining</th>
                    <th className="p-3">Line total</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items.map((item) => (
                    <tr key={item.id} className="border-t">
                      <td className="p-3">{item.sku}</td>
                      <td className="p-3">{item.productName} · {item.color} · {item.size}</td>
                      <td className="p-3">{item.quantity}</td>
                      <td className="p-3">{item.returnedQuantity}</td>
                      <td className="p-3">{item.remainingReturnableQuantity}</td>
                      <td className="p-3">₹{item.lineTotal.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>
    </AppShell>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-card p-4">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 font-semibold">{value}</p>
    </div>
  );
}
