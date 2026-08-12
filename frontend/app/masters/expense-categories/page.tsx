import { MasterDataPage } from "@/components/master-data-page";

export default function ExpenseCategoriesPage() {
  return <MasterDataPage config={{ title: "Expense Categories", singular: "Expense Category", endpoint: "/expense-categories" }} />;
}
