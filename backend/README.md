# Backend

Spring Boot REST API for the LUGAIRE ECOM MANAGEMENT system.

## What this service does

- Exposes the application API under `/api/v1`
- Uses PostgreSQL as the database
- Uses Flyway for schema migrations
- Uses Hibernate only for schema validation (`ddl-auto=validate`)
- Provides the backend for the Next.js frontend in `../frontend`

## Requirements

- Java 21 or newer
- Maven Wrapper (`mvnw` / `mvnw.cmd`) is committed in the repo
- PostgreSQL 16 or newer

## Environment files

This backend reads configuration from:

- `backend/.env`
- `../.env`

You do not need both. Use whichever is easier for your setup.

### Create or edit the local backend env file

If `backend/.env` already exists, open it and update the values.
If it does not exist yet, create it from the example file:

```powershell
Copy-Item .env.example .env
```

### Required variables

Use these names in your `.env` file:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ecommerce_management
DB_SCHEMA=lugaire
DB_USERNAME=ecommerce
DB_PASSWORD=your_real_postgres_password
CORS_ALLOWED_ORIGINS=http://localhost:3000
AUTH_BOOTSTRAP_ENABLED=true
AUTH_BOOTSTRAP_NAME=Administrator
AUTH_BOOTSTRAP_EMAIL=admin@lugaire.local
AUTH_BOOTSTRAP_PASSWORD=lugai.re
AUTH_BOOTSTRAP_ROLE=ADMIN
```

Notes:

- Do not leave `DB_PASSWORD` empty if PostgreSQL requires a password.
- `AUTH_BOOTSTRAP_PASSWORD` is only for the first administrator seed account.
- Default bootstrap login:
  - Email: `admin@lugaire.local`
  - Password: `lugai.re`
- The repository does not commit a real `.env` file; create one locally from `.env.example` if needed.

## Connecting to Supabase

Supabase API keys like `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, and `SUPABASE_SECRET_KEY` are **not** the same thing as the PostgreSQL connection string Spring Boot needs.

To sync local changes directly into Supabase, point this backend at the Supabase Postgres database by setting:

```properties
DB_URL=jdbc:postgresql://db.tjwmqaiwwcadgzpgofcu.supabase.co:5432/postgres?sslmode=require&currentSchema=lugaire
DB_USERNAME=postgres
DB_PASSWORD=your_supabase_database_password
```

If you prefer, you can keep using the individual `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` values instead of `DB_URL`.

Important:

- `DB_URL` takes priority over `DB_HOST` / `DB_PORT` / `DB_NAME`
- `sslmode=require` should stay enabled for Supabase
- the password must be the **Supabase database password**, not the API secret key

## Run the backend

From the `backend` folder:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

The API usually starts on:

- `http://localhost:8080`

## Verify the service

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Version check:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/version
```

## Database behavior

- Database name expected by default: `ecommerce_management`
- Database user expected by default: `ecommerce`
- PostgreSQL port expected by default: `5432`
- Default schema expected by the app: `lugaire`
- Flyway creates and manages the schema
- Hibernate validates the schema and does not create tables

If you use Docker:

```powershell
cd ..
docker compose up -d postgres
```

The default Docker database values come from the root `.env` file or from the defaults in `docker-compose.yml`.

## API endpoints

### Foundation

- `GET /api/v1/health`
- `GET /api/v1/version`

### Authentication

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

### Dashboard

- `GET /api/v1/dashboard/summary`

### Users and roles

- `GET /api/v1/roles`
- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `PATCH /api/v1/users/{id}/active`

### Master data

- `GET /api/v1/brands`
- `POST /api/v1/brands`
- `PUT /api/v1/brands/{id}`
- `PATCH /api/v1/brands/{id}/active`
- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `PATCH /api/v1/categories/{id}/active`
- `GET /api/v1/colors`
- `POST /api/v1/colors`
- `PUT /api/v1/colors/{id}`
- `PATCH /api/v1/colors/{id}/active`
- `GET /api/v1/sizes`
- `POST /api/v1/sizes`
- `PUT /api/v1/sizes/{id}`
- `PATCH /api/v1/sizes/{id}/active`
- `GET /api/v1/expense-categories`

### Marketplaces

- `GET /api/v1/marketplaces`
- `POST /api/v1/marketplaces`
- `PUT /api/v1/marketplaces/{id}`
- `PATCH /api/v1/marketplaces/{id}/active`

### Products and pricing

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `PATCH /api/v1/products/{id}/status`
- `GET /api/v1/pricing`
- `GET /api/v1/pricing/{variantId}`
- `GET /api/v1/pricing/{variantId}/history`
- `PUT /api/v1/pricing/{variantId}`

### Inventory

- `GET /api/v1/inventory`
- `GET /api/v1/inventory/low-stock`
- `GET /api/v1/inventory/out-of-stock`
- `GET /api/v1/inventory/{variantId}`
- `GET /api/v1/inventory/{variantId}/detail`
- `GET /api/v1/inventory/{variantId}/movements`
- `POST /api/v1/inventory/adjustments`
- `POST /api/v1/inventory/transactions`

### Orders

- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `POST /api/v1/orders`
- `PUT /api/v1/orders/{id}`

### Purchases

- `GET /api/v1/purchases`
- `GET /api/v1/purchases/{id}`
- `POST /api/v1/purchases`
- `PUT /api/v1/purchases/{id}`
- `PATCH /api/v1/purchases/{id}/status`
- `PATCH /api/v1/purchases/{id}/payment-status`

### Returns

- `GET /api/v1/returns`
- `GET /api/v1/returns/{id}`
- `POST /api/v1/returns`
- `PATCH /api/v1/returns/{id}/status`

### Settlements

- `GET /api/v1/settlements`
- `GET /api/v1/settlements/{id}`
- `POST /api/v1/settlements`
- `PUT /api/v1/settlements/{id}`
- `PATCH /api/v1/settlements/{id}/status`
- `PATCH /api/v1/settlements/{id}/reconcile`

### Expenses

- `GET /api/v1/expenses`
- `GET /api/v1/expenses/{id}`
- `POST /api/v1/expenses`
- `PUT /api/v1/expenses/{id}`
- `PATCH /api/v1/expenses/{id}/payment-status`

### Reports

- `GET /api/v1/reports/financial`
- `GET /api/v1/reports/marketplaces`
- `GET /api/v1/reports/expenses`
- `GET /api/v1/reports/products`

### Import / export

- `GET /api/v1/import-export/{module}/template`
- `GET /api/v1/import-export/{module}/export`
- `POST /api/v1/import-export/{module}/import`

### Audit logs

- `GET /api/v1/audit-logs`
- `GET /api/v1/audit-logs/{id}`
- `GET /api/v1/audit-logs/module/{module}`
- `GET /api/v1/audit-logs/entity/{entityType}/{entityId}`

### Settings

- `GET /api/v1/settings`
- `GET /api/v1/settings/{key}`
- `PUT /api/v1/settings/{key}`

## Common startup issues

- If you see `The server requested SCRAM-based authentication, but no password was provided`, set `DB_PASSWORD` in `backend/.env` or in the root `.env`.
- If PostgreSQL runs in Docker, make sure the password in the app env matches the password in `docker-compose.yml`.
- If the backend starts but health checks fail, verify PostgreSQL is running and the database name is correct.

## Useful files

- `src/main/resources/application.yml` — backend configuration
- `.env.example` — local backend env template
- `../docker-compose.yml` — optional PostgreSQL container
