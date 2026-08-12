import { MasterDataPage } from "@/components/master-data-page";
export default function CategoriesPage() { return <MasterDataPage config={{ title: "Categories", singular: "Category", endpoint: "/categories", extraField: "groupName", extraLabel: "Group" }} />; }
