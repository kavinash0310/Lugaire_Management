"use client";

import { useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { api } from "@/lib/api";
import { ArrowUpDown, Download, FileText, Upload } from "lucide-react";

const modules = [
  { value: "products", label: "Products / SKUs", importable: true, help: "Best for product and SKU master data." },
  { value: "inventory", label: "Inventory", importable: true, help: "Use for opening stock and manual adjustments." },
  { value: "orders", label: "Orders", importable: false, help: "Export only." },
  { value: "purchases", label: "Purchases", importable: false, help: "Export only." },
  { value: "expenses", label: "Expenses", importable: true, help: "Use for finance imports with category and supplier codes." },
] as const;

const formats = [
  { value: "xlsx", label: "Excel (.xlsx)" },
  { value: "csv", label: "CSV (.csv)" },
] as const;

type ImportResult = {
  module: string;
  processed: number;
  created: number;
  updated: number;
  failed: number;
  errors: Array<{ rowNumber: number; message: string }>;
};

export default function ImportExportPage() {
  const [selectedModule, setSelectedModule] = useState<(typeof modules)[number]["value"]>("products");
  const [selectedFormat, setSelectedFormat] = useState<(typeof formats)[number]["value"]>("xlsx");
  const [file, setFile] = useState<File | null>(null);
  const [loadingAction, setLoadingAction] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [result, setResult] = useState<ImportResult | null>(null);

  const moduleInfo = useMemo(() => modules.find((entry) => entry.value === selectedModule) ?? modules[0], [selectedModule]);

  const downloadBlob = (blob: Blob, filename: string) => {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  };

  const runDownload = async (kind: "export" | "template") => {
    setLoadingAction(kind);
    setMessage("");
    setResult(null);
    try {
      const response = await api.get(`/import-export/${selectedModule}/${kind}`, {
        params: { format: selectedFormat },
        responseType: "blob",
      });
      downloadBlob(response.data, `${selectedModule}-${kind}.${selectedFormat}`);
      setMessage(`${kind === "export" ? "Export" : "Template"} download started successfully.`);
    } catch {
      setMessage(`Unable to download ${kind}.`);
    } finally {
      setLoadingAction(null);
    }
  };

  const runImport = async () => {
    if (!file) {
      setMessage("Please choose a file first.");
      return;
    }
    setLoadingAction("import");
    setMessage("");
    setResult(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const response = await api.post<ImportResult>(`/import-export/${selectedModule}/import`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setResult(response.data);
      setMessage("Import completed.");
    } catch {
      setMessage("Import failed.");
    } finally {
      setLoadingAction(null);
    }
  };

  return (
    <AppShell>
      <section className="mx-auto max-w-5xl p-5 md:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Utilities</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Import / Export</h1>
            <p className="mt-2 text-sm text-muted-foreground">Download templates, export data, or import supported master and finance files.</p>
          </div>
          <div className="inline-flex items-center gap-2 rounded-full border bg-muted px-3 py-2 text-xs font-medium text-muted-foreground">
            <ArrowUpDown className="size-4" />
            {moduleInfo.help}
          </div>
        </div>

        <div className="mt-6 grid gap-4 rounded-xl border bg-card p-5 shadow-sm md:grid-cols-2">
          <label className="grid gap-2 text-sm">
            <span className="font-medium">Module</span>
            <select value={selectedModule} onChange={(event) => { setSelectedModule(event.target.value as typeof selectedModule); setFile(null); setResult(null); setMessage(""); }} className="rounded-md border bg-background px-3 py-2">
              {modules.map((entry) => <option key={entry.value} value={entry.value}>{entry.label}</option>)}
            </select>
          </label>

          <label className="grid gap-2 text-sm">
            <span className="font-medium">Format</span>
            <select value={selectedFormat} onChange={(event) => setSelectedFormat(event.target.value as typeof selectedFormat)} className="rounded-md border bg-background px-3 py-2">
              {formats.map((entry) => <option key={entry.value} value={entry.value}>{entry.label}</option>)}
            </select>
          </label>

          <div className="grid gap-2 text-sm">
            <span className="font-medium">Download</span>
            <div className="flex flex-wrap gap-3">
              <button type="button" onClick={() => void runDownload("export")} disabled={loadingAction !== null} className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-60">
                <Download className="size-4" />
                Export data
              </button>
              <button type="button" onClick={() => void runDownload("template")} disabled={loadingAction !== null || !moduleInfo.importable} className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium text-foreground disabled:opacity-60">
                <FileText className="size-4" />
                Download template
              </button>
            </div>
          </div>

          <div className="grid gap-2 text-sm">
            <span className="font-medium">Import</span>
            <div className="flex flex-wrap items-center gap-3">
              <label className="inline-flex cursor-pointer items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium">
                <Upload className="size-4" />
                <input type="file" accept=".csv,.xlsx" className="hidden" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
                {file ? file.name : "Choose file"}
              </label>
              <button type="button" onClick={() => void runImport()} disabled={!moduleInfo.importable || !file || loadingAction !== null} className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60">
                Import file
              </button>
            </div>
          </div>
        </div>

        {message && <p className="mt-4 rounded-md border bg-muted px-4 py-3 text-sm">{message}</p>}

        {result && (
          <div className="mt-6 rounded-xl border bg-card p-5 shadow-sm">
            <h2 className="text-lg font-semibold">Import result</h2>
            <div className="mt-4 grid gap-3 text-sm md:grid-cols-4">
              <div className="rounded-lg bg-muted p-3">
                <p className="text-muted-foreground">Processed</p>
                <p className="text-xl font-semibold">{result.processed}</p>
              </div>
              <div className="rounded-lg bg-muted p-3">
                <p className="text-muted-foreground">Created</p>
                <p className="text-xl font-semibold">{result.created}</p>
              </div>
              <div className="rounded-lg bg-muted p-3">
                <p className="text-muted-foreground">Updated</p>
                <p className="text-xl font-semibold">{result.updated}</p>
              </div>
              <div className="rounded-lg bg-muted p-3">
                <p className="text-muted-foreground">Failed</p>
                <p className="text-xl font-semibold">{result.failed}</p>
              </div>
            </div>

            {result.errors.length > 0 && (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-muted text-muted-foreground">
                    <tr>
                      <th className="px-3 py-2">Row</th>
                      <th className="px-3 py-2">Message</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.errors.map((error, index) => (
                      <tr key={`${error.rowNumber}-${index}`} className="border-t">
                        <td className="px-3 py-2">{error.rowNumber}</td>
                        <td className="px-3 py-2">{error.message}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </section>
    </AppShell>
  );
}
