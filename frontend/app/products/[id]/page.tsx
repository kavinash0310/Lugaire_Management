import { ProductDetails } from "@/components/product-details";
export default async function ProductPage({params}:{params:Promise<{id:string}>}){const {id}=await params;return <ProductDetails id={id}/>;}
