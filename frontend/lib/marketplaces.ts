"use client";

import { api } from "@/lib/api";

export type Marketplace = {
  id: number;
  name: string;
  code: string;
  active: boolean;
  defaultCommissionRate: string | number;
  defaultShippingCharge: string | number;
  remarks?: string | null;
};

export async function fetchMarketplaces() {
  const response = await api.get<Marketplace[]>("/marketplaces");
  return response.data;
}
