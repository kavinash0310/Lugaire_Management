"use client";

import { FormEvent, useMemo, useState } from "react";

type OrderOption = {
  id: number;
  orderId: string;
  platform: string;
  orderDate: string;
  orderStatus: string;
  paymentStatus: string;
};

type SettlementItemForm = {
  orderId: string;
  grossOrderAmount: string;
  marketplaceFee: string;
  shippingCharge: string;
  returnCharge: string;
  otherCharge: string;
  settledAmount: string;
  remarks: string;
};

type SettlementFormValues = {
  settlementId: string;
  platform: string;
  settlementDate: string;
  settlementPeriodStart: string;
  settlementPeriodEnd: string;
  grossAmount: string;
  marketplaceFees: string;
  shippingCharges: string;
  returnCharges: string;
  otherCharges: string;
  receivedAmount: string;
  remarks: string;
  items: SettlementItemForm[];
};

type SettlementRequestPayload = {
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
  remarks: string;
  items: Array<{
    orderId: number;
    grossOrderAmount: number;
    marketplaceFee: number;
    shippingCharge: number;
    returnCharge: number;
    otherCharge: number;
    settledAmount: number;
    remarks: string;
  }>;
};

const blankItem = (): SettlementItemForm => ({
  orderId: "",
  grossOrderAmount: "",
  marketplaceFee: "0",
  shippingCharge: "0",
  returnCharge: "0",
  otherCharge: "0",
  settledAmount: "0",
  remarks: "",
});

const today = new Date().toISOString().slice(0, 10);

export function SettlementForm({
  initialValues,
  orderOptions,
  onSubmit,
  submitLabel,
  }: {
  initialValues?: Partial<SettlementFormValues>;
  orderOptions: OrderOption[];
  onSubmit: (payload: SettlementRequestPayload) => Promise<void>;
  submitLabel: string;
}) {
  const initialItems = initialValues?.items?.length ? initialValues.items : [blankItem()];
  const [form, setForm] = useState<SettlementFormValues>({
    settlementId: "",
    platform: "MEESHO",
    settlementDate: today,
    settlementPeriodStart: today,
    settlementPeriodEnd: today,
    grossAmount: "0",
    marketplaceFees: "0",
    shippingCharges: "0",
    returnCharges: "0",
    otherCharges: "0",
    receivedAmount: "0",
    remarks: "",
    ...initialValues,
    items: initialItems,
  });
  const [search, setSearch] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const filteredOrders = useMemo(() => {
    const query = search.toLowerCase();
    return orderOptions.filter((order) => `${order.orderId} ${order.platform} ${order.orderDate} ${order.orderStatus} ${order.paymentStatus}`.toLowerCase().includes(query));
  }, [orderOptions, search]);

  const totals = useMemo(() => {
    const gross = form.items.reduce((sum, item) => sum + Number(item.grossOrderAmount || 0), 0);
    const fees = form.items.reduce((sum, item) => sum + Number(item.marketplaceFee || 0), 0);
    const shipping = form.items.reduce((sum, item) => sum + Number(item.shippingCharge || 0), 0);
    const returns = form.items.reduce((sum, item) => sum + Number(item.returnCharge || 0), 0);
    const other = form.items.reduce((sum, item) => sum + Number(item.otherCharge || 0), 0);
    const received = form.items.reduce((sum, item) => sum + Number(item.settledAmount || 0), 0);
    const net = gross - fees - shipping - returns - other;
    return { gross, fees, shipping, returns, other, received, net, difference: received - net };
  }, [form.items]);

  const setField = (key: keyof SettlementFormValues, value: string) => setForm((current) => ({ ...current, [key]: value }));
  const setItem = (index: number, patch: Partial<SettlementItemForm>) => setForm((current) => ({ ...current, items: current.items.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item) }));
  const addItem = () => setForm((current) => ({ ...current, items: [...current.items, blankItem()] }));
  const removeItem = (index: number) => setForm((current) => ({ ...current, items: current.items.filter((_, itemIndex) => itemIndex !== index) }));

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!form.settlementId.trim()) {
      setError("Settlement ID is required.");
      return;
    }
    if (!form.items.length || form.items.some((item) => !item.orderId)) {
      setError("Please select at least one existing order for the settlement.");
      return;
    }
    setSaving(true);
    try {
      await onSubmit({
        settlementId: form.settlementId.trim(),
        platform: form.platform,
        settlementDate: form.settlementDate,
        settlementPeriodStart: form.settlementPeriodStart,
        settlementPeriodEnd: form.settlementPeriodEnd,
        grossAmount: totals.gross,
        marketplaceFees: totals.fees,
        shippingCharges: totals.shipping,
        returnCharges: totals.returns,
        otherCharges: totals.other,
        receivedAmount: totals.received,
        remarks: form.remarks,
        items: form.items.map((item) => ({
          orderId: Number(item.orderId),
          grossOrderAmount: Number(item.grossOrderAmount),
          marketplaceFee: Number(item.marketplaceFee),
          shippingCharge: Number(item.shippingCharge),
          returnCharge: Number(item.returnCharge),
          otherCharge: Number(item.otherCharge),
          settledAmount: Number(item.settledAmount),
          remarks: item.remarks,
        })),
      });
    } catch {
      setError("Unable to save settlement. Please check the order selections and amounts.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={submit} className="mt-6 space-y-6 rounded-lg border bg-card p-6 shadow-sm">
      {error && <p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="grid gap-4 md:grid-cols-3">
        <Field label="Settlement ID" value={form.settlementId} onChange={(value) => setField("settlementId", value)} />
        <Select label="Platform" value={form.platform} onChange={(value) => setField("platform", value)} options={["MEESHO", "AMAZON", "FLIPKART", "WEBSITE", "OTHER"].map((value) => ({ value, label: value }))} />
        <Field type="date" label="Settlement date" value={form.settlementDate} onChange={(value) => setField("settlementDate", value)} />
        <Field type="date" label="Period start" value={form.settlementPeriodStart} onChange={(value) => setField("settlementPeriodStart", value)} />
        <Field type="date" label="Period end" value={form.settlementPeriodEnd} onChange={(value) => setField("settlementPeriodEnd", value)} />
        <Field readOnly type="number" label="Gross amount" value={totals.gross.toFixed(2)} onChange={(value) => setField("grossAmount", value)} />
        <Field readOnly type="number" label="Marketplace fees" value={totals.fees.toFixed(2)} onChange={(value) => setField("marketplaceFees", value)} />
        <Field readOnly type="number" label="Shipping charges" value={totals.shipping.toFixed(2)} onChange={(value) => setField("shippingCharges", value)} />
        <Field readOnly type="number" label="Return charges" value={totals.returns.toFixed(2)} onChange={(value) => setField("returnCharges", value)} />
        <Field readOnly type="number" label="Other charges" value={totals.other.toFixed(2)} onChange={(value) => setField("otherCharges", value)} />
        <Field readOnly type="number" label="Received amount" value={totals.received.toFixed(2)} onChange={(value) => setField("receivedAmount", value)} />
      </div>

      <div className="rounded border bg-muted/20 p-4 text-sm">
        <div className="grid gap-2 md:grid-cols-3">
          <p><strong>Expected net:</strong> ₹{totals.net.toFixed(2)}</p>
          <p><strong>Received:</strong> ₹{totals.received.toFixed(2)}</p>
          <p><strong>Difference:</strong> ₹{totals.difference.toFixed(2)}</p>
        </div>
      </div>

      <div className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">Settlement items</h2>
            <p className="text-sm text-muted-foreground">Select only existing orders. Backend validation prevents duplicates and fake references.</p>
          </div>
          <button type="button" onClick={addItem} className="rounded border px-3 py-2 text-sm">Add item</button>
        </div>

        <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search existing orders" className="w-full rounded border bg-background px-3 py-2 text-sm" />

        {form.items.map((item, index) => {
          const selectedOrder = orderOptions.find((order) => String(order.id) === item.orderId);
          const expectedNet = Number(item.grossOrderAmount || 0) - Number(item.marketplaceFee || 0) - Number(item.shippingCharge || 0) - Number(item.returnCharge || 0) - Number(item.otherCharge || 0);
          const difference = Number(item.settledAmount || 0) - expectedNet;
          return (
            <div key={index} className="space-y-3 rounded border p-4">
              <div className="grid gap-3 md:grid-cols-3">
                <Select
                  label="Order"
                  value={item.orderId}
                  onChange={(value) => setItem(index, { orderId: value })}
                  options={filteredOrders.map((order) => ({ value: String(order.id), label: `${order.orderId} · ${order.platform} · ${order.orderDate}` }))}
                />
                <Field type="number" label="Gross order amount" value={item.grossOrderAmount} onChange={(value) => setItem(index, { grossOrderAmount: value })} />
                <Field type="number" label="Marketplace fee" value={item.marketplaceFee} onChange={(value) => setItem(index, { marketplaceFee: value })} />
                <Field type="number" label="Shipping charge" value={item.shippingCharge} onChange={(value) => setItem(index, { shippingCharge: value })} />
                <Field type="number" label="Return charge" value={item.returnCharge} onChange={(value) => setItem(index, { returnCharge: value })} />
                <Field type="number" label="Other charge" value={item.otherCharge} onChange={(value) => setItem(index, { otherCharge: value })} />
                <Field type="number" label="Settled amount" value={item.settledAmount} onChange={(value) => setItem(index, { settledAmount: value })} />
                <div className="flex items-end">
                  <button type="button" onClick={() => removeItem(index)} disabled={form.items.length === 1} className="rounded border px-3 py-2 text-sm disabled:opacity-50">Remove</button>
                </div>
              </div>
              <div className="grid gap-2 text-sm text-muted-foreground md:grid-cols-3">
                <p><strong>Expected net:</strong> ₹{expectedNet.toFixed(2)}</p>
                <p><strong>Difference:</strong> ₹{difference.toFixed(2)}</p>
                <p><strong>Selected order:</strong> {selectedOrder ? `${selectedOrder.orderId} (${selectedOrder.paymentStatus})` : "None"}</p>
              </div>
              <label className="block text-sm font-medium">
                Remarks
                <textarea value={item.remarks} onChange={(event) => setItem(index, { remarks: event.target.value })} className="mt-1 min-h-20 w-full rounded border bg-background px-3 py-2" />
              </label>
            </div>
          );
        })}
      </div>

      <label className="block text-sm font-medium">
        Remarks
        <textarea value={form.remarks} onChange={(event) => setField("remarks", event.target.value)} className="mt-1 min-h-24 w-full rounded border bg-background px-3 py-2" />
      </label>

      <div className="flex justify-end">
        <button disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-white disabled:opacity-50">
          {saving ? "Saving..." : submitLabel}
        </button>
      </div>
    </form>
  );
}

function Field({ label, value, onChange, type = "text", readOnly = false }: { label: string; value: string; onChange: (value: string) => void; type?: string; readOnly?: boolean }) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <input
        type={type}
        required
        readOnly={readOnly}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 block w-full rounded border bg-background px-3 py-2 font-normal disabled:opacity-50"
      />
    </label>
  );
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }> }) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 block w-full rounded border bg-background px-3 py-2 font-normal">
        <option value="">Select</option>
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
    </label>
  );
}
