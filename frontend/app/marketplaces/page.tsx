"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";

type Marketplace = {
  id: number;
  name: string;
  code: string;
  active: boolean;
  defaultCommissionRate: number;
  defaultShippingCharge: number;
  remarks?: string | null;
};

type FormState = {
  name: string;
  code: string;
  defaultCommissionRate: string;
  defaultShippingCharge: string;
  remarks: string;
  active: boolean;
};

const initialForm: FormState = {
  name: "",
  code: "",
  defaultCommissionRate: "0",
  defaultShippingCharge: "0",
  remarks: "",
  active: true,
};

export default function MarketplacesPage() {
  const [items, setItems] = useState<Marketplace[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState<Marketplace | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<Marketplace[]>("/marketplaces");
      setItems(response.data);
    } catch {
      setError("Unable to load marketplaces.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const filtered = useMemo(
    () => items.filter((item) => `${item.name} ${item.code}`.toLowerCase().includes(search.toLowerCase())),
    [items, search],
  );

  const openCreate = () => {
    setEditing(null);
    setForm(initialForm);
    setShowForm(true);
  };

  const openEdit = (item: Marketplace) => {
    setEditing(item);
    setForm({
      name: item.name,
      code: item.code,
      defaultCommissionRate: String(item.defaultCommissionRate),
      defaultShippingCharge: String(item.defaultShippingCharge),
      remarks: item.remarks ?? "",
      active: item.active,
    });
    setShowForm(true);
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const payload = {
        name: form.name.trim(),
        code: form.code.trim(),
        active: form.active,
        defaultCommissionRate: Number(form.defaultCommissionRate),
        defaultShippingCharge: Number(form.defaultShippingCharge),
        remarks: form.remarks.trim() || null,
      };
      if (editing) {
        await api.put(`/marketplaces/${editing.id}`, payload);
      } else {
        await api.post("/marketplaces", payload);
      }
      setShowForm(false);
      await load();
    } catch {
      setError("Unable to save marketplace. Please check the code and required fields.");
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (item: Marketplace) => {
    if (!window.confirm(`${item.active ? "Deactivate" : "Activate"} ${item.name}?`)) return;
    try {
      await api.patch(`/marketplaces/${item.id}/active`, { active: !item.active });
      await load();
    } catch {
      setError("Unable to update marketplace status.");
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Masters</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Marketplaces</h1>
          </div>
          <button onClick={openCreate} className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800">
            Add marketplace
          </button>
        </div>

        <div className="mt-6 rounded-lg border bg-card shadow-sm">
          <div className="border-b p-4">
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search marketplaces..."
              className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-slate-300"
            />
          </div>
          {error && <p className="m-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
          {loading ? (
            <p className="p-6 text-sm text-muted-foreground">Loading marketplaces...</p>
          ) : filtered.length === 0 ? (
            <p className="p-6 text-sm text-muted-foreground">No marketplaces found. Add one to get started.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted text-muted-foreground">
                  <tr>
                    <th className="px-5 py-3 font-medium">Name</th>
                    <th className="px-5 py-3 font-medium">Code</th>
                    <th className="px-5 py-3 font-medium">Commission</th>
                    <th className="px-5 py-3 font-medium">Shipping</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                    <th className="px-5 py-3 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((item) => (
                    <tr key={item.id} className="border-t">
                      <td className="px-5 py-3 font-medium">{item.name}</td>
                      <td className="px-5 py-3">{item.code}</td>
                      <td className="px-5 py-3">{item.defaultCommissionRate}</td>
                      <td className="px-5 py-3">{item.defaultShippingCharge}</td>
                      <td className="px-5 py-3">
                        <span className={`rounded-full px-2 py-1 text-xs font-medium ${item.active ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-600"}`}>
                          {item.active ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td className="space-x-3 px-5 py-3 text-right">
                        <button onClick={() => openEdit(item)} className="text-slate-700 hover:underline">Edit</button>
                        <button onClick={() => void toggle(item)} className="text-slate-700 hover:underline">
                          {item.active ? "Deactivate" : "Activate"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {showForm && (
          <div className="fixed inset-0 z-10 grid place-items-center bg-slate-950/30 p-4">
            <form onSubmit={save} className="w-full max-w-md rounded-lg bg-card p-6 shadow-xl">
              <h2 className="text-lg font-semibold">{editing ? "Edit marketplace" : "Add marketplace"}</h2>
              <div className="mt-5 space-y-4">
                <Field label="Name" value={form.name} onChange={(value) => setForm({ ...form, name: value })} />
                <Field label="Code" value={form.code} onChange={(value) => setForm({ ...form, code: value.toUpperCase() })} />
                <Field label="Default commission rate" type="number" value={form.defaultCommissionRate} onChange={(value) => setForm({ ...form, defaultCommissionRate: value })} />
                <Field label="Default shipping charge" type="number" value={form.defaultShippingCharge} onChange={(value) => setForm({ ...form, defaultShippingCharge: value })} />
                <label className="block text-sm font-medium">
                  Remarks
                  <textarea
                    value={form.remarks}
                    onChange={(event) => setForm({ ...form, remarks: event.target.value })}
                    className="mt-1 w-full rounded-md border bg-background px-3 py-2 font-normal outline-none focus:ring-2 focus:ring-slate-300"
                  />
                </label>
                <label className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} />
                  Active
                </label>
              </div>
              <div className="mt-6 flex justify-end gap-3">
                <button type="button" onClick={() => setShowForm(false)} className="rounded-md border px-4 py-2 text-sm">Cancel</button>
                <button disabled={saving} className="rounded-md bg-slate-900 px-4 py-2 text-sm text-white disabled:opacity-50">
                  {saving ? "Saving..." : "Save"}
                </button>
              </div>
            </form>
          </div>
        )}
      </section>
    </AppShell>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
}) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <input
        required
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 w-full rounded-md border bg-background px-3 py-2 font-normal outline-none focus:ring-2 focus:ring-slate-300"
      />
    </label>
  );
}
