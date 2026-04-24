# Motif

Dropwizard-based REST server with OPAQUE (RFC 9807) authentication via the [hofmann-elimination](https://github.com/codeheadsystems/hofmann-elimination) library.

## Prerequisites

- Java 21+
- Docker and Docker Compose
- Node.js 18+ and npm (for building the webapp)

## Quick Start with Docker Compose

The fastest way to get Motif running locally:

### 1. Build the project

```bash
./gradlew build
```

This compiles all modules, builds the webapp (Vite), bundles static assets into the server JAR, and runs all tests.

### 2. Create your local `.env`

`docker-compose` reads OPAQUE/JWT secrets from a `.env` file (gitignored). A template with placeholder values lives at `.env.example`:

```bash
cp .env.example .env
# optional but recommended: replace the placeholders with real randoms
for v in MOTIF_OPAQUE_SERVER_KEY_SEED_HEX MOTIF_OPAQUE_OPRF_SEED_HEX \
         MOTIF_OPAQUE_OPRF_MASTER_KEY_HEX MOTIF_JWT_SECRET_HEX; do
  echo "$v=$(openssl rand -hex 32)"
done > .env
```

Never commit `.env`. In production these values come from AWS Secrets Manager.

### 3. Start the system

```bash
docker compose up --build
```

This starts PostgreSQL and the Motif server. The entrypoint runs `init-db` to apply Flyway migrations, then starts the server with the secrets from `.env`.

### 4. Open the webapp

Navigate to `http://localhost:8080/app` to register a user and log in.

### Optional: LocalStack for AWS development

The compose file includes an opt-in LocalStack service for developing CDK infrastructure and any future AWS SDK integrations locally (S3 attachments, Secrets Manager, etc.):

```bash
docker compose --profile aws up -d localstack
cd infra
npm install
npm run bootstrap:local
npm run deploy:local
```

LocalStack Community covers S3, IAM, Secrets Manager, CloudFormation, CloudWatch Logs. Aurora, ECS Fargate, CloudFront, and X-Ray require LocalStack Pro and are validated against real AWS only. See `infra/README.md` for details.

### Stopping

```bash
docker compose down
```

To also remove the database volume (resets all data):

```bash
docker compose down -v
```

## Quick Start (Manual / No Docker)

### 1. Start PostgreSQL

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

# Secrets read from env vars (see ### 3 below)
serverKeySeedHex: ${MOTIF_OPAQUE_SERVER_KEY_SEED_HEX}
oprfSeedHex: ${MOTIF_OPAQUE_OPRF_SEED_HEX}
oprfMasterKeyHex: ${MOTIF_OPAQUE_OPRF_MASTER_KEY_HEX}
jwtSecretHex: ${MOTIF_JWT_SECRET_HEX}

context: motif-local-v1
allowIdentityKsf: true
argon2MemoryKib: 0
```

Setting `argon2MemoryKib: 0` with `allowIdentityKsf: true` disables Argon2 key stretching for fast local development. **Do not use this in production.**

### 3. Export secrets

```bash
export MOTIF_OPAQUE_SERVER_KEY_SEED_HEX=$(openssl rand -hex 32)
export MOTIF_OPAQUE_OPRF_SEED_HEX=$(openssl rand -hex 32)
export MOTIF_OPAQUE_OPRF_MASTER_KEY_HEX=$(openssl rand -hex 32)
export MOTIF_JWT_SECRET_HEX=$(openssl rand -hex 32)
```

### 4. Build the project

```bash
./gradlew build
```

### 5. Initialize the database

```bash
java -jar server/build/libs/server.jar init-db config.yml
```

This applies Flyway migrations to create all tables. It is safe to re-run.

### 6. Start the server

```bash
java -jar server/build/libs/server.jar server config.yml
```

The server starts on `http://localhost:8080`. The webapp is served at `http://localhost:8080/app`.

### 7. (Optional) Webapp dev mode

For live-reload during frontend development:

```bash
cd webapp
npm run dev
```

This starts a Vite dev server at `http://localhost:5173` that proxies `/opaque`, `/oprf`, and `/api` requests to the Motif server on port 8080. The server must be running for the proxy to work.

## Configuration

Dropwizard reads YAML with `${ENV_VAR}` substitution. Secrets (OPAQUE seeds, OPRF master key, JWT signing secret) live in environment variables — never in YAML, never in the database.

Required secret env vars (each is a 32-byte hex string; generate with `openssl rand -hex 32`):

| Env var | Purpose |
|---|---|
| `MOTIF_OPAQUE_SERVER_KEY_SEED_HEX` | OPAQUE server AKE key seed |
| `MOTIF_OPAQUE_OPRF_SEED_HEX` | OPRF seed |
| `MOTIF_OPAQUE_OPRF_MASTER_KEY_HEX` | OPRF master key (non-zero P-256 scalar) |
| `MOTIF_JWT_SECRET_HEX` | JWT HMAC-SHA256 signing secret |

If any of these are missing or blank at startup, the server fails fast with a message listing what's missing.

**In production**: values come from AWS Secrets Manager and are injected as ECS task env vars.

**In docker-compose**: values come from `.env` (gitignored), template in `.env.example`.

**In tests**: values are randomly generated per JVM and passed via Dropwizard `ConfigOverride`.

The `configuration_values` database table is retained for non-secret runtime configuration (feature flags, tunable thresholds, defaults). Adding secrets to it is prohibited — code review should reject any such changes.

## Database Initialization

Before starting the server for the first time, run the `init-db` command:

```bash
java -jar server/build/libs/server.jar init-db config.yml
```

This applies Flyway migrations to create all tables. Safe to re-run after new migrations are added.

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

## CRITICAL

**DO NOT RELEASE THIS APPLICATION EVER WITHOUT FIXING THIS!**

### HTTPS in production

HTTPS must be enforced in production to protect the JWT in transit. Production plan: TLS termination at the AWS Application Load Balancer (see `docs/technical_architecture.md`).

### Database credentials

Database credentials are currently in plaintext YAML in dev. Production plan: AWS Secrets Manager, injected as ECS task env vars at startup.

