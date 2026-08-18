# Frontend

Next.js + React + TypeScript frontend for the LUGAIRE ECOM MANAGEMENT system.

## What this app does

- Provides the dashboard shell and navigation
- Calls the backend REST API through Axios
- Uses Tailwind CSS and shadcn/ui-style component patterns where needed
- Reads its API base URL from an environment variable

## Requirements

- Node.js 20 or newer
- npm
- Backend API running at `http://localhost:8080` or another URL you configure

## Environment file

If `frontend/.env.local` already exists, edit it.
If it does not exist yet, create it from the example:

```powershell
Copy-Item .env.example .env.local
```

Required variable:

```properties
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
```

If you change the backend port or host, update this value.

## Run the frontend

From the `frontend` folder:

```powershell
npm install
npm run dev
```

The app usually starts on:

- `http://localhost:3000`

## Verify the frontend

```powershell
npm run lint
npm run build
```

## API client

The frontend API client lives at:

- `lib/api.ts`

It uses:

- `NEXT_PUBLIC_API_BASE_URL`
- a fallback of `/api/v1` if the environment variable is missing

## Main routes

### Core

- `/` — dashboard shell
- `/login` — login page

### Master data

- `/masters/brands`
- `/masters/categories`
- `/masters/colors`
- `/masters/sizes`
- `/masters/expense-categories`

### Commerce modules

- `/products`
- `/products/new`
- `/products/[id]`
- `/pricing`
- `/inventory`
- `/inventory/[variantId]`
- `/orders`
- `/orders/new`
- `/orders/[id]`
- `/orders/[id]/edit`
- `/purchases`
- `/purchases/new`
- `/purchases/[id]`
- `/purchases/[id]/edit`
- `/returns`
- `/returns/new`
- `/returns/[id]`
- `/settlements`
- `/settlements/new`
- `/settlements/[id]`
- `/settlements/[id]/edit`
- `/expenses`
- `/expenses/new`
- `/expenses/[id]`
- `/expenses/[id]/edit`

### Supporting pages

- `/marketplaces`
- `/reports`
- `/settings`
- `/users`
- `/audit-logs`
- `/audit-logs/[id]`
- `/import-export`

## Project structure

- `app/` — Next.js App Router pages and layout
- `components/` — shared UI and shell components
- `lib/` — helpers such as the Axios API client

## Notes

- Most screens are organized as route-level pages so the app can grow phase by phase.
- The frontend expects the backend to expose `/api/v1` endpoints.
- If the backend URL changes, update `frontend/.env.local`.
