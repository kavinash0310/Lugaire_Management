"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type OrderDetail = {
  orderId: string;
  platform: string;
  orderStatus: string;
  paymentStatus: string;
  netAmount: number;
  items: Array<{ variantId: number; sku: string; quantity: number; lineTotal: number }>;
};

export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const [order, setOrder] = useState<OrderDetail>();
  const [error, setError] = useState("");
  const [orderId, setOrderId] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    params
      .then(({ id }) => {
        setOrderId(id);
        return api.get<OrderDetail>(`/orders/${id}`);
      })
      .then((response) => setOrder(response.data))
      .catch(() => setError("Unable to load this order."));
  }, [params]);

  async function updateStatus(status: string) {
    setSaving(true);
    setError("");
    try {
      await api.patch(`/orders/${orderId}/status`, { status });
      setOrder((current) => current ? { ...current, orderStatus: status } : current);
    } catch {
      setError("Unable to update the order status.");
    } finally {
      setSaving(false);
    }
  }

  async function updatePaymentStatus(status: string) {
    setSaving(true);
    setError("");
    try {
      await api.patch(`/orders/${orderId}/payment-status`, { status });
      setOrder((current) => current ? { ...current, paymentStatus: status } : current);
    } catch {
      setError("Unable to update the payment status.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-6">
        {!order && !error && <p>Loading order...</p>}
        {error && <p className="text-destructive">{error}</p>}
        {order && (
          <>
            <h1 className="text-2xl font-semibold">{order.orderId}</h1>
            <p className="mt-2 text-muted-foreground">
              {order.platform} · {order.orderStatus} · {order.paymentStatus}
            </p>
            <div className="mt-4 flex flex-wrap gap-3">
              <Link className="rounded border px-3 py-2 text-sm" href={`/orders/${orderId}/edit`}>Edit order</Link>
              <label className="text-sm">
                Order status
                <select
                  className="ml-2 rounded border bg-background p-2"
                  disabled={saving}
                  value={order.orderStatus}
                  onChange={(event) => updateStatus(event.target.value)}
                >
                  {['PENDING', 'PROCESSING', 'ON_THE_WAY', 'DELIVERED', 'CANCELLED', 'RTO', 'RETURNED'].map((status) => <option key={status}>{status}</option>)}
                </select>
              </label>
              <label className="text-sm">
                Payment status
                <select
                  className="ml-2 rounded border bg-background p-2"
                  disabled={saving}
                  value={order.paymentStatus}
                  onChange={(event) => updatePaymentStatus(event.target.value)}
                >
                  {['PENDING', 'PAID', 'DEDUCTED'].map((status) => <option key={status}>{status}</option>)}
                </select>
              </label>
            </div>
            <div className="mt-6 overflow-hidden rounded border">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted">
                  <tr><th className="p-3">SKU</th><th className="p-3">Quantity</th><th className="p-3">Line total</th></tr>
                </thead>
                <tbody>
                  {order.items.map((item) => (
                    <tr key={item.variantId} className="border-t">
                      <td className="p-3">{item.sku}</td><td className="p-3">{item.quantity}</td><td className="p-3">₹{item.lineTotal}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-6 text-lg font-semibold">Net amount: ₹{order.netAmount}</p>
          </>
        )}
      </section>
    </AppShell>
  );
}
