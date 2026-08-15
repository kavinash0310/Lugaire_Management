"use client";

import { type FormEvent, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { useAuth } from "@/components/auth-provider";
import { api } from "@/lib/api";
import { type AppSetting, settingsToMap } from "@/lib/settings";

type SettingFieldType = "text" | "number" | "select" | "textarea" | "checkbox";

type SettingField = {
  key: string;
  label: string;
  type: SettingFieldType;
  section: "BUSINESS" | "GENERAL" | "INVENTORY" | "ORDERS" | "FINANCIAL";
  options?: Array<{ value: string; label: string }>;
};

const fields: SettingField[] = [
  { key: "business_name", label: "Business Name", type: "text", section: "BUSINESS" },
  { key: "brand_name", label: "Brand Name", type: "text", section: "BUSINESS" },
  { key: "business_email", label: "Business Email", type: "text", section: "BUSINESS" },
  { key: "business_phone", label: "Business Phone", type: "text", section: "BUSINESS" },
  { key: "business_address", label: "Business Address", type: "textarea", section: "BUSINESS" },
  { key: "currency", label: "Currency", type: "text", section: "GENERAL" },
  { key: "currency_symbol", label: "Currency Symbol", type: "text", section: "GENERAL" },
  { key: "date_format", label: "Date Format", type: "text", section: "GENERAL" },
  { key: "default_low_stock_threshold", label: "Low Stock Threshold", type: "number", section: "INVENTORY" },
  { key: "allow_negative_stock", label: "Allow Negative Stock", type: "checkbox", section: "INVENTORY" },
  {
    key: "default_order_status",
    label: "Default Order Status",
    type: "select",
    section: "ORDERS",
    options: ["PENDING", "PROCESSING", "ON_THE_WAY", "DELIVERED", "RETURNED", "RTO", "CANCELLED"].map((value) => ({ value, label: value })),
  },
  {
    key: "default_payment_status",
    label: "Default Payment Status",
    type: "select",
    section: "ORDERS",
    options: ["PENDING", "PAID", "DEDUCTED"].map((value) => ({ value, label: value })),
  },
  { key: "default_tax_rate", label: "Default Tax Rate", type: "number", section: "FINANCIAL" },
];

const sections = [
  { key: "BUSINESS", title: "Business" },
  { key: "GENERAL", title: "General" },
  { key: "INVENTORY", title: "Inventory" },
  { key: "ORDERS", title: "Orders" },
  { key: "FINANCIAL", title: "Financial" },
] as const;

export default function SettingsPage() {
  const { user } = useAuth();
  const [settings, setSettings] = useState<Record<string, string>>({});
  const [original, setOriginal] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await api.get<AppSetting[]>("/settings");
        const next = settingsToMap(response.data);
        setSettings(next);
        setOriginal(next);
      } catch {
        setError("Unable to load settings.");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  const groupedFields = useMemo(
    () =>
      sections.map((section) => ({
        ...section,
        fields: fields.filter((field) => field.section === section.key),
      })),
    [],
  );

  const canEdit = user?.roleCode === "ADMIN";

  const updateField = (key: string, value: string) => {
    setSettings((current) => ({ ...current, [key]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!canEdit) {
      return;
    }
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const changes = fields.filter((field) => settings[field.key] !== original[field.key]);
      const savedEntries = await Promise.all(
        changes.map(async (field) => {
          const response = await api.put<AppSetting>(`/settings/${field.key}`, { value: settings[field.key] ?? "" });
          return response.data;
        }),
      );
      if (savedEntries.length > 0) {
        const next = { ...original };
        savedEntries.forEach((entry) => {
          next[entry.key] = entry.value;
        });
        setSettings(next);
        setOriginal(next);
      }
      setSuccess("Settings saved successfully.");
    } catch {
      setError("Unable to save settings.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-5xl p-5 md:p-8">
        <div>
          <p className="text-sm font-medium text-muted-foreground">Configuration</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">System Settings</h1>
          <p className="mt-2 text-sm text-muted-foreground">Centralized business and application configuration.</p>
        </div>

        {error && <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {success && <p className="mt-4 rounded-md bg-green-50 p-3 text-sm text-green-700">{success}</p>}

        {loading ? (
          <p className="mt-6 text-sm text-muted-foreground">Loading settings...</p>
        ) : (
          <form onSubmit={handleSubmit} className="mt-6 space-y-6">
            {groupedFields.map((section) => (
              <div key={section.key} className="rounded-xl border bg-card p-5 shadow-sm">
                <h2 className="text-lg font-semibold">{section.title}</h2>
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                  {section.fields.map((field) => (
                    <SettingInput
                      key={field.key}
                      field={field}
                      value={settings[field.key] ?? ""}
                      disabled={!canEdit}
                      onChange={updateField}
                    />
                  ))}
                </div>
              </div>
            ))}

            {canEdit ? (
              <button
                type="submit"
                disabled={saving}
                className="rounded-md bg-slate-900 px-4 py-2.5 text-sm font-medium text-white disabled:opacity-60"
              >
                {saving ? "Saving..." : "Save Settings"}
              </button>
            ) : (
              <p className="text-sm text-muted-foreground">Only administrators can change settings.</p>
            )}
          </form>
        )}
      </section>
    </AppShell>
  );
}

function SettingInput({
  field,
  value,
  disabled,
  onChange,
}: {
  field: SettingField;
  value: string;
  disabled: boolean;
  onChange: (key: string, value: string) => void;
}) {
  return (
    <div className="space-y-2">
      <span className="block text-sm font-medium">{field.label}</span>
      {field.type === "textarea" ? (
        <textarea
          value={value}
          disabled={disabled}
          onChange={(event) => onChange(field.key, event.target.value)}
          rows={4}
          className="w-full rounded-md border bg-background px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-muted"
        />
      ) : field.type === "select" ? (
        <select
          value={value}
          disabled={disabled}
          onChange={(event) => onChange(field.key, event.target.value)}
          className="w-full rounded-md border bg-background px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-muted"
        >
          {(field.options ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : field.type === "checkbox" ? (
        <label className="flex items-center gap-3 rounded-md border px-3 py-2">
          <input
            type="checkbox"
            checked={value === "true"}
            disabled={disabled}
            onChange={(event) => onChange(field.key, String(event.target.checked))}
            className="size-4"
          />
          <span className="text-sm text-muted-foreground">Enabled</span>
        </label>
      ) : (
        <input
          type={field.type}
          value={value}
          disabled={disabled}
          onChange={(event) => onChange(field.key, event.target.value)}
          className="w-full rounded-md border bg-background px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-muted"
        />
      )}
    </div>
  );
}
