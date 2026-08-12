import { MasterDataPage } from "@/components/master-data-page";
export default function SizesPage() { return <MasterDataPage config={{ title: "Sizes", singular: "Size", endpoint: "/sizes", extraField: "sizeType", extraLabel: "Size type" }} />; }
