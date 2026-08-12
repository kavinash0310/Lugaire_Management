# LUGAIRE ECOM MANAGEMENT

Phase 1 establishes a clean foundation for a future e-commerce business-management application. It intentionally contains no business modules, CRUD functionality, authentication, or business database tables.

## Architecture

- `frontend/`: Next.js + React + TypeScript application providing the responsive application shell.
- `backend/`: Spring Boot modular-monolith API. Phase 1 exposes only operational endpoints.
- PostgreSQL is the source of truth. Flyway owns schema migrations, while Hibernate only validates the resulting schema.
- The frontend communicates with the backend via REST, using Axios through a single API client.

Future business capabilities should be added as cohesive backend packages and matching frontend feature folders, without splitting the system into microservices.

## Project structure

```text
.
├── backend/                 # Spring Boot REST API
├── frontend/                # Next.js web application
├── docker-compose.yml       # Optional local PostgreSQL
├── .env.example             # Docker database environment template
└── README.md
```

## Prerequisites

- Node.js 20.9+
- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (or Docker Desktop)

## Run locally

1. Create the root environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

   Set a strong local password in `.env` before starting PostgreSQL.

2. Start PostgreSQL (optional if using your own instance):

   ```powershell
   docker compose up -d postgres
   ```

3. Configure and start the backend:

   ```powershell
   Copy-Item backend/.env.example backend/.env
   # Edit backend/.env with your PostgreSQL credentials if needed.
   cd backend
   mvn spring-boot:run
   ```

   The API runs at `http://localhost:8080`. Check `http://localhost:8080/api/v1/health` and `http://localhost:8080/api/v1/version`.

4. Configure and start the frontend in a second terminal:

   ```powershell
   Copy-Item frontend/.env.example frontend/.env.local
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:3000`.

## Verification commands

```powershell
# Frontend
cd frontend
npm run lint
npm run build

# Backend
cd ../backend
mvn test
mvn package

# Health API (when backend is running)
Invoke-RestMethod http://localhost:8080/api/v1/health
```

## Phase 1 completion checklist

- [x] Separate Next.js frontend and Spring Boot backend
- [x] Responsive sidebar and dashboard placeholder
- [x] Axios API client with frontend environment configuration
- [x] PostgreSQL and Flyway environment configuration
- [x] `ddl-auto=validate` enabled
- [x] Flyway baseline migration, without business tables
- [x] Health and version API endpoints
- [x] Consistent API error response handling
- [x] Git-friendly ignore and line-ending configuration

Phase 2 and later features are intentionally out of scope.
