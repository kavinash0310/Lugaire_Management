import { MasterDataPage } from "@/components/master-data-page";
export default function BrandsPage() { return <MasterDataPage config={{ title: "Brands", singular: "Brand", endpoint: "/brands" }} />; }
