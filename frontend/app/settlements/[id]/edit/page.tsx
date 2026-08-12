"use client";

import Link from "next/link";
import { use, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { SettlementForm } from "@/components/settlement-form";
import { api } from "@/lib/api";

type OrderOption = {
  id: number;
  orderId: string;
  platform: string;
  orderDate: string;
  orderStatus: string;
  paymentStatus: string;
};

type OrderPage = { content: OrderOption[] };

type SettlementDetail = {
  settlementId: string;
  platform: string;
  settlementDate: string;
  settlementPeriodStart: string;
  settlementPeriodEnd: string;
  grossAmount: number;
  marketplaceFees: number;
  shippingCharges: number;
  returnCharges: number;
  otherCharges: number;
  receivedAmount: number;
  remarks?: string;
  items: Array<{
    orderId: number;
    grossOrderAmount: number;
    marketplaceFee: number;
    shippingCharge: number;
    returnCharge: number;
    otherCharge: number;
    settledAmount: number;
    remarks?: string;
  }>;
};

export default function EditSettlementPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [orders, setOrders] = useState<OrderOption[]>([]);
  const [initial, setInitial] = useState<SettlementDetail | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<OrderPage>("/orders?size=200").then((response) => setOrders(response.data.content)).catch(() => setOrders([]));
    api.get<SettlementDetail>(`/settlements/${id}`).then((response) => setInitial(response.data)).catch(() => setError("Unable to load settlement."));
  }, [id]);

  if (error) {
    return <AppShell><section className="mx-auto max-w-7xl p-5 md:p-8"><p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p></section></AppShell>;
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold">Edit settlement</h1>
          </div>
          <Link href={`/settlements/${id}`} className="text-sm underline">Back to settlement</Link>
        </div>

        {!initial ? <p className="mt-6 text-sm text-muted-foreground">Loading settlement...</p> : (
          <SettlementForm
            initialValues={{
              settlementId: initial.settlementId,
              platform: initial.platform,
              settlementDate: initial.settlementDate,
              settlementPeriodStart: initial.settlementPeriodStart,
              settlementPeriodEnd: initial.settlementPeriodEnd,
              grossAmount: String(initial.grossAmount),
              marketplaceFees: String(initial.marketplaceFees),
              shippingCharges: String(initial.shippingCharges),
              returnCharges: String(initial.returnCharges),
              otherCharges: String(initial.otherCharges),
              receivedAmount: String(initial.receivedAmount),
              remarks: initial.remarks ?? "",
              items: initial.items.map((item) => ({
                orderId: String(item.orderId),
                grossOrderAmount: String(item.grossOrderAmount),
                marketplaceFee: String(item.marketplaceFee),
                shippingCharge: String(item.shippingCharge),
                returnCharge: String(item.returnCharge),
                otherCharge: String(item.otherCharge),
                settledAmount: String(item.settledAmount),
                remarks: item.remarks ?? "",
              })),
            }}
            orderOptions={orders}
            submitLabel="Update settlement"
            onSubmit={async (payload) => {
              await api.put(`/settlements/${id}`, payload);
              window.location.href = `/settlements/${id}`;
            }}
          />
        )}
      </section>
    </AppShell>
  );
}
