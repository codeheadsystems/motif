# Security TODO

Findings from security review conducted 2026-04-05.

## CRITICAL

- [x] **IDOR in SubjectManager.update() — ownership not verified**
  `SubjectManager.update()` uses `findByIdentifier()` (no owner filter) to check existence.
  An authenticated user could modify another user's subject by guessing UUIDs.
  Compare with `EventManager.update()` and `NoteManager.update()` which correctly use
  `findByOwnerAndIdentifier()`.
  **Files:** `server-db/.../manager/SubjectManager.java:66-79`
  **Fix:** Use `findByOwnerAndIdentifier()` in the update transaction.

- [ ] **OPAQUE keys stored in PostgreSQL**
  All cryptographic secrets (`serverKeySeedHex`, `oprfSeedHex`, `jwtSecretHex`) are in the
  `configuration_values` table. Database compromise = full auth compromise.
  README already warns: "DO NOT RELEASE THIS APPLICATION EVER WITHOUT FIXING THIS!"
  **Fix:** Move to external vault (HSM, AWS KMS, HashiCorp Vault). Requires architectural change.

## HIGH

- [x] **Missing security headers on static assets (/app/\*)**
  API endpoints return `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`,
  etc. but the AssetServlet serving `/app/*` does not. Enables clickjacking via iframe embedding.
  **Files:** `server/.../MotifApplication.java:57-60`
  **Fix:** Add a servlet filter that applies security headers to all responses including static assets.

- [x] **No input validation in resource create/update methods**
  `SubjectResource.create()`, `EventResource.create()`, `NoteResource.create()` extract values
  from `Map` bodies without null checks or length validation. Malformed input returns 500
  (Internal Server Error) instead of 400 (Bad Request), leaking stack traces.
  **Files:** `server/.../resource/SubjectResource.java:70-74`,
  `server/.../resource/EventResource.java:85-101`,
  `server/.../resource/NoteResource.java:84-105`
  **Fix:** Add null/type checks, return 400 for invalid input.

- [x] **Weak Argon2 parameters**
  `docker/config.yml` sets `argon2MemoryKib: 0` and `allowIdentityKsf: true`, providing no
  memory-hard protection for password hashing.
  **Fix:** Set `argon2MemoryKib: 16384` (16 MiB), `argon2Iterations: 3`, disable `allowIdentityKsf`.
  16 MiB chosen for mobile compatibility (OPAQUE KSF runs client-side).

- [ ] **Admin port (8081) exposed without authentication**
  Dropwizard admin port exposes `/healthcheck`, `/threads`, `/metrics` unauthenticated.
  Thread dumps leak internal class names and Java version.
  **Fix:** Restrict admin port to localhost/internal network in deployment config, or add auth.

## MEDIUM

- [ ] **No maximum page size validation**
  `PageRequest` validates `pageSize >= 1` but has no upper bound. A user could request
  `?size=2147483647`, causing OOM or heavy DB load.
  **Files:** `common/.../PageRequest.java`
  **Fix:** Add upper bound (e.g., 200) in PageRequest constructor.

- [ ] **Weak frontend password policy**
  Registration only checks password is non-empty (`length < 1`). No minimum length or
  complexity requirements.
  **Files:** `webapp/src/app.ts`
  **Fix:** Enforce minimum 8-character password in frontend and backend.

- [ ] **JWT stored in sessionStorage (XSS-stealable)**
  If any XSS vulnerability exists, the token can be stolen via
  `sessionStorage.getItem('motif_jwt')`. HttpOnly cookies would be more secure.
  **Files:** `webapp/src/auth.ts`
  **Fix:** Consider migrating to HttpOnly, SameSite cookie-based sessions.

- [ ] **No HTTPS in default configuration**
  `docker/config.yml` uses plain HTTP. JWT tokens transmitted in cleartext.
  **Fix:** Add TLS termination via reverse proxy (nginx) or Dropwizard TLS config.

## LOW

- [ ] **No audit logging**
  No logging of authentication attempts, password changes, or data modifications.
  **Fix:** Add audit log table and log critical events.

- [ ] **No session cleanup job**
  Expired sessions and pending sessions accumulate in the database.
  **Fix:** Add scheduled cleanup task for expired sessions.

- [ ] **Jetty error pages leak server information**
  Path traversal attempts return detailed Jetty error messages revealing server type.
  **Fix:** Configure custom error pages in Dropwizard.

- [ ] **Database credentials in plaintext config**
  `docker/config.yml` has `databasePassword: motif` in cleartext.
  **Fix:** Use environment variables or secrets management in production.
