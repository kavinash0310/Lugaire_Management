"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { SettlementForm } from "@/components/settlement-form";
import { api } from "@/lib/api";
import { fetchMarketplaces, type Marketplace } from "@/lib/marketplaces";

type OrderOption = {
  id: number;
  orderId: string;
  platform: string;
  orderDate: string;
  orderStatus: string;
  paymentStatus: string;
};

type OrderPage = { content: OrderOption[] };

export default function NewSettlementPage() {
  const [orders, setOrders] = useState<OrderOption[]>([]);
  const [marketplaces, setMarketplaces] = useState<Marketplace[]>([]);

  useEffect(() => {
    api.get<OrderPage>("/orders?size=200").then((response) => setOrders(response.data.content)).catch(() => setOrders([]));
    fetchMarketplaces().then(setMarketplaces).catch(() => setMarketplaces([]));
  }, []);

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold">Create settlement</h1>
          </div>
          <Link href="/settlements" className="text-sm underline">Back to settlements</Link>
        </div>

        <SettlementForm
          orderOptions={orders}
          marketplaces={marketplaces}
          submitLabel="Create settlement"
          onSubmit={async (payload) => {
            await api.post("/settlements", payload);
            window.location.href = "/settlements";
          }}
        />
      </section>
    </AppShell>
  );
}
