export type DashboardSummary = {
  businessName: string;
  currencySymbol: string;
  period: string;
  fromDate: string;
  toDate: string;
  revenue: {
    grossSales: number;
    deliveredSales: number;
    returnedSales: number;
    rtoSales: number;
    cancelledSales: number;
    orders: number;
    deliveredOrders: number;
    returnedOrders: number;
    rtoOrders: number;
    cancelledOrders: number;
  };
  costs: {
    productCost: number;
    marketplaceFees: number;
    shippingCharges: number;
    returnCharges: number;
    otherCharges: number;
    returnLoss: number;
    settlementReceived: number;
    settlementDifference: number;
  };
  expenses: {
    totalExpenses: number;
    paidExpenses: number;
    pendingExpenses: number;
  };
  profit: {
    grossProfit: number;
    netProfit: number;
    profitMargin: number;
  };
  backlog: {
    pendingPayments: number;
    pendingSettlements: number;
  };
  returns: {
    returnCount: number;
    rtoCount: number;
    returnRate: number;
    rtoRate: number;
  };
  salesTrend: DashboardTrendPoint[];
  profitTrend: DashboardTrendPoint[];
  orderStatusBreakdown: DashboardStatusBreakdown[];
  marketplaces: DashboardMarketplace[];
  lowStock: DashboardInventory[];
  expenseBreakdown: DashboardExpenseBreakdown[];
  generatedAt: string;
};

export type DashboardTrendPoint = {
  date: string;
  value: number;
  count: number;
};

export type DashboardStatusBreakdown = {
  status: string;
  count: number;
};

export type DashboardMarketplace = {
  platform: string;
  orders: number;
  sales: number;
  returnsRto: number;
  marketplaceFees: number;
  shippingCharges: number;
  returnCharges: number;
  settlements: number;
  netRevenue: number;
  profit: number;
};

export type DashboardInventory = {
  variantId: number;
  sku: string;
  productName: string;
  brandName: string;
  categoryName: string;
  color: string;
  size: string;
  currentStock: number;
  lowStockThreshold: number;
  status: string;
  unitCost: number;
  inventoryValue: number;
};

export type DashboardExpenseBreakdown = {
  categoryId: number;
  categoryName: string;
  amount: number;
  percentage: number;
};
