"use client";

import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";

type Supplier = {
  id: number;
  supplierId: string;
  name: string;
  contactPerson?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  city?: string | null;
  state?: string | null;
  gstin?: string | null;
  notes?: string | null;
  active: boolean;
};

type SupplierPage = {
  content: Supplier[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

const emptyForm = {
  supplierId: "",
  name: "",
  contactPerson: "",
  phone: "",
  email: "",
  address: "",
  city: "",
  state: "",
  gstin: "",
  notes: "",
};

export default function SuppliersPage() {
  const [data, setData] = useState<SupplierPage>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [search, setSearch] = useState("");
  const [active, setActive] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      const query = new URLSearchParams({ search, page: String(page), size: "20" });
      if (active !== "") query.set("active", active);
      try {
        const response = await api.get<SupplierPage>(`/suppliers?${query.toString()}`);
        setData(response.data);
      } catch {
        setError("Unable to load suppliers.");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [search, active, page]);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (supplier: Supplier) => {
    setEditing(supplier);
    setForm({
      supplierId: supplier.supplierId,
      name: supplier.name,
      contactPerson: supplier.contactPerson ?? "",
      phone: supplier.phone ?? "",
      email: supplier.email ?? "",
      address: supplier.address ?? "",
      city: supplier.city ?? "",
      state: supplier.state ?? "",
      gstin: supplier.gstin ?? "",
      notes: supplier.notes ?? "",
    });
    setShowForm(true);
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (editing) {
        await api.put(`/suppliers/${editing.id}`, form);
      } else {
        await api.post("/suppliers", form);
      }
      setShowForm(false);
      const query = new URLSearchParams({ search, page: String(page), size: "20" });
      if (active !== "") query.set("active", active);
      const response = await api.get<SupplierPage>(`/suppliers?${query.toString()}`);
      setData(response.data);
    } catch {
      setError("Unable to save supplier. Check the supplier code and required fields.");
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (supplier: Supplier) => {
    if (!window.confirm(`${supplier.active ? "Deactivate" : "Activate"} ${supplier.name}?`)) return;
    try {
      await api.patch(`/suppliers/${supplier.id}/active`, { active: !supplier.active });
      const query = new URLSearchParams({ search, page: String(page), size: "20" });
      if (active !== "") query.set("active", active);
      const response = await api.get<SupplierPage>(`/suppliers?${query.toString()}`);
      setData(response.data);
    } catch {
      setError("Unable to update supplier status.");
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl p-5 md:p-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Procurement</p>
            <h1 className="mt-1 text-2xl font-semibold">Suppliers</h1>
          </div>
          <button onClick={openCreate} className="rounded bg-slate-900 px-4 py-2 text-white">
            Add supplier
          </button>
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          <input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} placeholder="Search suppliers" className="min-w-72 rounded border px-3 py-2" />
          <select value={active} onChange={(event) => { setActive(event.target.value); setPage(0); }} className="rounded border bg-background px-3 py-2">
            <option value="">All status</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
          <Link href="/purchases" className="rounded border px-4 py-2 text-sm">View purchases</Link>
        </div>

        {loading && <p className="mt-4">Loading suppliers...</p>}
        {error && <p className="mt-4 text-destructive">{error}</p>}

        {!loading && !error && (
          <>
            <div className="mt-4 overflow-x-auto rounded border bg-card">
              <table className="w-full text-left text-sm">
                <thead className="bg-muted">
                  <tr>
                    <th className="p-3">Supplier ID</th>
                    <th>Name</th>
                    <th>Contact</th>
                    <th>City</th>
                    <th>Status</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((supplier) => (
                    <tr key={supplier.id} className="border-t">
                      <td className="p-3 font-medium">{supplier.supplierId}</td>
                      <td>{supplier.name}</td>
                      <td>{supplier.contactPerson ?? supplier.phone ?? supplier.email ?? "-"}</td>
                      <td>{supplier.city ?? "-"}</td>
                      <td>{supplier.active ? "Active" : "Inactive"}</td>
                      <td className="space-x-3 p-3 text-right">
                        <button onClick={() => openEdit(supplier)} className="underline">Edit</button>
                        <button onClick={() => void toggleActive(supplier)} className="underline">
                          {supplier.active ? "Deactivate" : "Activate"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {data.content.length === 0 && <p className="mt-4 text-muted-foreground">No suppliers found.</p>}
            <div className="mt-4 flex items-center gap-3">
              <button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded border px-3 py-2 disabled:opacity-50">Previous</button>
              <span className="text-sm">Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}</span>
              <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((current) => current + 1)} className="rounded border px-3 py-2 disabled:opacity-50">Next</button>
            </div>
          </>
        )}

        {showForm && (
          <div className="fixed inset-0 z-10 grid place-items-center bg-slate-950/30 p-4">
            <form onSubmit={submit} className="w-full max-w-2xl rounded-lg bg-card p-6 shadow-xl">
              <h2 className="text-lg font-semibold">{editing ? "Edit supplier" : "Add supplier"}</h2>
              <div className="mt-5 grid gap-4 md:grid-cols-2">
                <Field label="Supplier ID" value={form.supplierId} onChange={(value) => setForm({ ...form, supplierId: value })} />
                <Field label="Name" value={form.name} onChange={(value) => setForm({ ...form, name: value })} />
                <Field label="Contact person" value={form.contactPerson} onChange={(value) => setForm({ ...form, contactPerson: value })} />
                <Field label="Phone" value={form.phone} onChange={(value) => setForm({ ...form, phone: value })} />
                <Field label="Email" value={form.email} onChange={(value) => setForm({ ...form, email: value })} />
                <Field label="GSTIN" value={form.gstin} onChange={(value) => setForm({ ...form, gstin: value })} />
                <Field label="City" value={form.city} onChange={(value) => setForm({ ...form, city: value })} />
                <Field label="State" value={form.state} onChange={(value) => setForm({ ...form, state: value })} />
                <Field label="Address" value={form.address} onChange={(value) => setForm({ ...form, address: value })} />
                <Field label="Notes" value={form.notes} onChange={(value) => setForm({ ...form, notes: value })} />
              </div>
              <div className="mt-6 flex justify-end gap-3">
                <button type="button" onClick={() => setShowForm(false)} className="rounded border px-4 py-2 text-sm">
                  Cancel
                </button>
                <button disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-sm text-white disabled:opacity-50">
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

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block text-sm font-medium">
      {label}
      <input
        required={label === "Supplier ID" || label === "Name"}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 w-full rounded border bg-background px-3 py-2 font-normal outline-none focus:ring-2 focus:ring-slate-300"
      />
    </label>
  );
}
