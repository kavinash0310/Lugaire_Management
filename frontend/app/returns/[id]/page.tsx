"use client";

import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type ReturnDetail = {
  id: number;
  orderId: string;
  orderPlatform: string;
  orderDate: string;
  orderStatus: string;
  productName: string;
  color: string;
  size: string;
  sku: string;
  type: string;
  reason: string;
  quantity: number;
  productCost: number;
  shippingLoss: number;
  otherLoss: number;
  totalLoss: number;
  resellable: boolean;
  status: string;
  returnDate: string;
  receivedDate?: string;
  remarks?: string;
  inventoryRestored: boolean;
};

export default function ReturnDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const [id, setId] = useState("");
  const [record, setRecord] = useState<ReturnDetail>();
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    params.then(async ({ id: recordId }) => {
      setId(recordId);
      try {
        const response = await api.get<ReturnDetail>(`/returns/${recordId}`);
        setRecord(response.data);
      } catch {
        setError("Unable to load this return record.");
      }
    });
  }, [params]);

  async function updateStatus(status: string) {
    setSaving(true);
    setError("");
    try {
      const response = await api.patch<ReturnDetail>(`/returns/${id}/status`, { status });
      setRecord(response.data);
    } catch {
      setError("Unable to update the return status.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-3xl p-6">
        {!record && !error && <p>Loading return record...</p>}
        {error && <p className="text-destructive">{error}</p>}
        {record && (
          <>
            <h1 className="text-2xl font-semibold">{record.type} - {record.orderId}</h1>
            <p className="mt-2 text-muted-foreground">{record.productName} / {record.color} / {record.size} / {record.sku} - Qty {record.quantity}</p>
            <div className="mt-6 grid gap-3 rounded border p-5 text-sm">
              <p><strong>Order:</strong> {record.orderId}</p>
              <p><strong>Platform:</strong> {record.orderPlatform}</p>
              <p><strong>Order date:</strong> {record.orderDate}</p>
              <p><strong>Order status:</strong> {record.orderStatus}</p>
              <p><strong>Reason:</strong> {record.reason}</p>
              <p><strong>Return date:</strong> {record.returnDate}</p>
              {record.receivedDate && <p><strong>Received date:</strong> {record.receivedDate}</p>}
              <p><strong>Product cost:</strong> Rs. {record.productCost}</p>
              <p><strong>Shipping loss:</strong> Rs. {record.shippingLoss}</p>
              <p><strong>Other loss:</strong> Rs. {record.otherLoss}</p>
              <p><strong>Total loss:</strong> Rs. {record.totalLoss}</p>
              <p><strong>Resellable:</strong> {record.resellable ? "Yes" : "No"}</p>
              <p><strong>Inventory restored:</strong> {record.inventoryRestored ? "Yes" : "No"}</p>
              {record.remarks && <p><strong>Remarks:</strong> {record.remarks}</p>}
            </div>
            <label className="mt-5 block text-sm">
              Status
              <select value={record.status} disabled={saving} onChange={(event) => updateStatus(event.target.value)} className="ml-3 rounded border bg-background p-2">
                {["INITIATED", "RECEIVED", "INSPECTED", "COMPLETED"].map((value) => <option key={value}>{value}</option>)}
              </select>
            </label>
          </>
        )}
      </section>
    </AppShell>
  );
}
