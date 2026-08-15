import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/v1",
  headers: { Accept: "application/json" },
  withCredentials: true,
});
