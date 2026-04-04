# Motif Server

Dropwizard-based REST server with OPAQUE (RFC 9807) authentication via the [hofmann-elimination](https://github.com/codeheadsystems/hofmann-elimination) library.

## Prerequisites

- Java 21+
- PostgreSQL 16+

## Configuration

Create a `config.yml` with your database connection details:

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
databasePassword: <your-password>

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

## Running the Server

```bash
java -jar server.jar server config.yml
```

## OPAQUE Authentication Endpoints

Once running, the following endpoints are available:

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

## Architecture

- **server** — Dropwizard application, Dagger DI, store implementations, CLI commands
- **server-db** — JDBI DAOs, Flyway migrations, model records
- **common** — Shared utilities (Configuration, PageRequest, etc.)

The OPAQUE stores (credentials, sessions, pending sessions) are backed by PostgreSQL via JDBI, ensuring data survives server restarts.
