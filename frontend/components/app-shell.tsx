"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ArrowUpDown, BarChart3, Boxes, ClipboardList, CreditCard, DollarSign, History, LayoutDashboard, LogOut, Menu, Package, Palette, RotateCcw, Ruler, Settings, Store, Tags, Truck, UserCog, Users, Warehouse } from "lucide-react";
import { type ReactNode, useEffect, useMemo, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { api } from "@/lib/api";
import { type AppSetting } from "@/lib/settings";

const navigation = [
  { label: "Dashboard", icon: LayoutDashboard, href: "/" },
  { label: "Products", icon: Package, href: "/products" },
  { label: "Pricing", icon: DollarSign, href: "/pricing" },
  { label: "Inventory", icon: Boxes, href: "/inventory" },
  { label: "Marketplaces", icon: Store, href: "/marketplaces" },
  { label: "Suppliers", icon: Warehouse, href: "/suppliers" },
  { label: "Purchases", icon: ClipboardList, href: "/purchases" },
  { label: "Orders", icon: Truck, href: "/orders" },
  { label: "Returns & RTO", icon: RotateCcw, href: "/returns" },
  { label: "Settlements", icon: CreditCard, href: "/settlements" },
  { label: "Expenses", icon: DollarSign, href: "/expenses" },
  { label: "Import / Export", icon: ArrowUpDown, href: "/import-export" },
  { label: "Settings", icon: Settings, href: "/settings" },
  { label: "Audit Logs", icon: History, href: "/audit-logs" },
  { label: "Reports", icon: BarChart3, href: "/reports" },
];

const masters = [
  { label: "Brands", icon: Tags, href: "/masters/brands" },
  { label: "Categories", icon: ClipboardList, href: "/masters/categories" },
  { label: "Colors", icon: Palette, href: "/masters/colors" },
  { label: "Sizes", icon: Ruler, href: "/masters/sizes" },
  { label: "Expense Categories", icon: ClipboardList, href: "/masters/expense-categories" },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, status, logout } = useAuth();
  const [businessName, setBusinessName] = useState("LUGAIRE ECOM MANAGEMENT");

  useEffect(() => {
    if (status === "unauthenticated" && pathname !== "/login") {
      router.replace("/login");
    }
  }, [pathname, router, status]);

  useEffect(() => {
    if (status !== "authenticated") {
      return;
    }
    let mounted = true;
    api.get<AppSetting[]>("/settings")
      .then((response) => {
        const business = response.data.find((setting) => setting.key === "business_name");
        if (mounted && business?.value.trim()) {
          setBusinessName(business.value.trim());
        }
      })
      .catch(() => {});
    return () => {
      mounted = false;
    };
  }, [status]);

  const links = useMemo(
    () => (user?.roleCode === "ADMIN" ? [...navigation, { label: "Users", icon: Users, href: "/users" }] : navigation),
    [user?.roleCode],
  );

  if (status === "loading") {
    return <div className="grid min-h-screen place-items-center bg-background text-sm text-muted-foreground">Checking session...</div>;
  }

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen md:grid md:grid-cols-[248px_1fr]">
      <aside className="hidden border-r bg-card md:block">
        <div className="flex h-16 items-center border-b px-6 font-semibold tracking-tight">{businessName}</div>
        <nav className="space-y-1 p-3" aria-label="Main navigation">
          {links.map(({ label, icon: Icon, href }) => (
            <Link
              key={label}
              href={href}
              className={`flex items-center gap-3 rounded-md px-3 py-2.5 text-sm transition ${
                pathname === href ? "bg-muted text-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
              }`}
            >
              <Icon className="size-4" />
              {label}
            </Link>
          ))}
          <p className="px-3 pt-5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Masters</p>
          {masters.map(({ label, icon: Icon, href }) => (
            <Link
              key={label}
              href={href}
              className={`flex items-center gap-3 rounded-md px-3 py-2.5 text-sm transition ${
                pathname === href ? "bg-muted text-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
              }`}
            >
              <Icon className="size-4" />
              {label}
            </Link>
          ))}
        </nav>
      </aside>
      <main>
        <header className="flex h-16 items-center justify-between border-b bg-card px-5 md:px-8">
          <button className="md:hidden" aria-label="Open navigation">
            <Menu className="size-5" />
          </button>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <div className="hidden text-right sm:block">
              <p className="font-medium text-foreground">{user.name}</p>
              <p className="text-xs text-muted-foreground">
                <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-1 text-[11px] font-semibold uppercase tracking-wide">
                  <UserCog className="size-3.5" />
                  {user.roleName}
                </span>
              </p>
            </div>
            <button
              type="button"
              onClick={() => void logout().finally(() => router.replace("/login"))}
              className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm font-medium text-foreground transition hover:bg-muted"
            >
              <LogOut className="size-4" />
              Logout
            </button>
          </div>
        </header>
        {children}
      </main>
    </div>
  );
}
