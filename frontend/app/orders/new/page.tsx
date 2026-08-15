"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import { fetchMarketplaces, type Marketplace } from "@/lib/marketplaces";

export default function NewOrderPage() {
  const [marketplaces, setMarketplaces] = useState<Marketplace[]>([]);
  const [marketplaceId, setMarketplaceId] = useState("");
  const [orderId, setOrderId] = useState("");
  const [variantId, setVariantId] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [sellingPrice, setSellingPrice] = useState("");
  const [orderDate, setOrderDate] = useState(new Date().toISOString().slice(0, 10));
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchMarketplaces().then((items) => {
      setMarketplaces(items);
      setMarketplaceId((current) => current || String(items[0]?.id ?? ""));
    }).catch(() => setMarketplaces([]));
  }, []);

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage("");
    setSaving(true);
    try {
      await api.post("/orders", {
        orderId,
        marketplaceId: Number(marketplaceId),
        orderDate,
        commission: 0,
        shippingCharge: 0,
        otherCharges: 0,
        items: [
          {
            variantId: Number(variantId),
            quantity: Number(quantity),
            sellingPrice: Number(sellingPrice),
          },
        ],
      });
      setMessage("Order saved.");
    } catch {
      setMessage("Unable to save order. Check the marketplace, SKU variant, and values.");
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
            <h1 className="text-2xl font-semibold">Create order</h1>
          </div>
          <Link href="/orders" className="text-sm underline">Back to orders</Link>
        </div>
        <form onSubmit={save} className="mt-6 space-y-4 rounded border bg-card p-5">
          <label className="block text-sm font-medium">
            Marketplace
            <select
              required
              value={marketplaceId}
              onChange={(event) => setMarketplaceId(event.target.value)}
              className="mt-1 w-full rounded border bg-background p-2"
            >
              <option value="">Select marketplace</option>
              {marketplaces.map((marketplace) => (
                <option key={marketplace.id} value={marketplace.id}>
                  {marketplace.code} · {marketplace.name}
                </option>
              ))}
            </select>
          </label>
          <input required placeholder="Marketplace order ID" value={orderId} onChange={(event) => setOrderId(event.target.value)} className="w-full rounded border p-2" />
          <input required type="date" value={orderDate} onChange={(event) => setOrderDate(event.target.value)} className="w-full rounded border p-2" />
          <input required min="1" type="number" placeholder="SKU Variant ID" value={variantId} onChange={(event) => setVariantId(event.target.value)} className="w-full rounded border p-2" />
          <input required min="1" type="number" placeholder="Quantity" value={quantity} onChange={(event) => setQuantity(event.target.value)} className="w-full rounded border p-2" />
          <input required min="0" type="number" placeholder="Selling price" value={sellingPrice} onChange={(event) => setSellingPrice(event.target.value)} className="w-full rounded border p-2" />
          <button disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-white disabled:opacity-50">
            {saving ? "Saving..." : "Save order"}
          </button>
          {message && <p className="text-sm">{message}</p>}
        </form>
      </section>
    </AppShell>
  );
}
