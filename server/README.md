# Motif Server

Dropwizard-based REST server with OPAQUE (RFC 9807) authentication via the [hofmann-elimination](https://github.com/codeheadsystems/hofmann-elimination) library.

## Prerequisites

- Java 21+
- PostgreSQL 16+
- Node.js 18+ and npm (for building the webapp)

## Quick Start (Local Development)

### 1. Start PostgreSQL

Using Docker:

```bash
docker run -d --name motif-db \
  -e POSTGRES_DB=motif \
  -e POSTGRES_USER=motif \
  -e POSTGRES_PASSWORD=motif \
  -p 5432:5432 \
  postgres:latest
```

### 2. Create `config.yml`

Create a `config.yml` in the project root (this file is gitignored):

```yaml
server:
  applicationConnectors:
    - type: http
      port: 8080
  adminConnectors:
    - type: http
      port: 8081

databaseUrl: jdbc:postgresql://localhost:5432/motif
databaseUser: motif
databasePassword: motif

# OPAQUE settings — keys are loaded from the database.
# These non-secret defaults are fine for local development.
context: motif-local-v1
allowIdentityKsf: true
argon2MemoryKib: 0
```

Setting `argon2MemoryKib: 0` with `allowIdentityKsf: true` disables Argon2 key stretching for fast local development. **Do not use this in production.**

### 3. Build the project

```bash
./gradlew build
```

This compiles all modules, builds the webapp (Vite), bundles static assets into the server JAR, and runs all tests.

### 4. Initialize the database

```bash
java -jar server/build/libs/server.jar init-db config.yml
```

This runs Flyway migrations to create all tables, then generates and stores random OPAQUE cryptographic keys in the `configuration_values` table. It is safe to re-run — existing keys are not overwritten.

### 5. Start the server

```bash
java -jar server/build/libs/server.jar server config.yml
```

The server starts on `http://localhost:8080`. The webapp is served at `http://localhost:8080/app`.

### 6. (Optional) Webapp dev mode

For live-reload during frontend development:

```bash
cd webapp
npm run dev
```

This starts a Vite dev server at `http://localhost:5173` that proxies `/opaque`, `/oprf`, and `/api` requests to the Motif server on port 8080. The server must be running for the proxy to work.

## Configuration

```yaml
server:
  applicationConnectors:
    - type: http
      port: 8080
  adminConnectors:
    - type: http
      port: 8081

databaseUrl: jdbc:postgresql://localhost:5432/motif
databaseUser: motif
databasePassword: motif

# OPAQUE settings loaded from database — do not put keys here.
# Optional overrides (non-secret values only):
# opaqueCipherSuite: P256_SHA256
# context: motif-opaque-v1
# allowIdentityKsf: false
```

**Important:** Currently all cryptographic keys (server key seed, OPRF seed, JWT secret, etc.) are stored in the database `configuration_values` table — never in YAML or any file checked into git.
This will change in the future as we add support for external HSM or vaults and KMS providers, but for now the `init-db` command will generate and store random keys for you.

The reason it is bad to store these keys here is it removes the advantage of the
OPAQUE protocol being resistant to server breaches — if an attacker gets read access to the database, they can steal the keys and perform offline password guessing attacks
which is exactly what we want to avoid. But for development purposes, we have it
here until we add support for external vaults.

## Database Initialization

Before starting the server for the first time, run the `init-db` command to generate and store random keys:

```bash
java -jar server.jar init-db config.yml
```

This will:
1. Run Flyway migrations to create all tables
2. Generate cryptographically random 256-bit keys for OPAQUE and JWT
3. Store them in the `configuration_values` table
4. Skip any keys that already exist (safe to re-run)

**The server will not start if the database has not been initialized.** You will see an error listing the missing configuration keys.

## Running the Server

```bash
java -jar server.jar server config.yml
```

## OPAQUE Authentication Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/opaque/config` | Client configuration (cipher suite, Argon2 params) |
| `POST` | `/opaque/registration/start` | Begin user registration |
| `POST` | `/opaque/registration/finish` | Complete user registration |
| `DELETE` | `/opaque/registration` | Delete a registration |
| `POST` | `/opaque/auth/start` | Begin authentication (returns KE2) |
| `POST` | `/opaque/auth/finish` | Complete authentication (returns JWT) |

After authentication, use the returned JWT as a Bearer token:

```
Authorization: Bearer <jwt-token>
```

## Motif API Endpoints (Authenticated)

All `/api/*` endpoints require a valid JWT Bearer token.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/owner` | Get or create the authenticated owner |
| `GET` | `/api/subjects?category={cat}` | List subjects by category |
| `POST` | `/api/subjects` | Create a subject |
| `GET` | `/api/events?subject={id}` | List events for a subject |
| `POST` | `/api/events` | Create an event |
| `GET` | `/api/notes?subject={id}` | List notes for a subject |
| `POST` | `/api/notes` | Create a note |

## Webapp

The web application is served at `/app` and provides:

- **Registration and login** via OPAQUE (zero-knowledge password authentication)
- **Dashboard** showing the authenticated user's credential
- **Entity browser** for managing Subjects, Events, and Notes

## Architecture

- **server** — Dropwizard application, Dagger DI, store implementations, CLI commands, REST resources
- **server-db** — JDBI DAOs, Flyway migrations, model records, business logic managers
- **common** — Shared utilities (Configuration, PageRequest, etc.)
- **webapp** — Vite + TypeScript + Bootstrap SPA (built assets bundled into server JAR)

The OPAQUE stores (credentials, sessions, pending sessions) are backed by PostgreSQL via JDBI, ensuring data survives server restarts.

# BEFORE RELEASING

When this application is nearing completion, the OPAQUE keys need to be in a storage
system separate from the database. 

**DO NOT RELEASE THIS APPLICATION EVER WITHOUT FIXING THIS!**
