# Motif Technical Architecture
## System Design, Implementation Details, and Technical Strategy

---

## System Architecture Overview

### What is the technical architecture?

Motif is a multi-client system with one authoritative backend and two first-party clients. The free tier lives entirely on Android with no network dependency; signing up promotes a user into cloud-synced mode and unlocks the webapp.

**Android App** (free tier entry point, upgrades to synced):
- **Language**: Kotlin (Jetpack Compose UI, Java 17 runtime)
- **UI Framework**: Jetpack Compose (declarative)
- **Architecture Pattern**: MVVM with clean-architecture layering
- **Dependency Injection**: Dagger 2 (compile-time, shared discipline with the backend)
- **Local Database**: Room (SQLite abstraction)
- **Concurrency**: Coroutines and Flow
- **Background Work**: WorkManager (sync, periodic pattern detection)
- **Auth**: OPAQUE client via `hofmann-client` (the Java client library from the hofmann-elimination project) — drops into the Android JVM runtime directly, no port required

**Web App** (premium/business only):
- **Build Tool**: Vite
- **Framework**: React 19 with TypeScript
- **Routing**: React Router 7
- **Styling**: Tailwind CSS with Radix UI primitives
- **Auth**: OPAQUE client via the Hofmann JavaScript library (`hofmann-js`); session held in HttpOnly, SameSite=Strict cookie
- **Testing**: Vitest (unit), Playwright (E2E, planned)

**Backend** (premium/business):
- **Language**: Java 17 (LTS)
- **HTTP Framework**: Dropwizard 4 (Jakarta JAX-RS, Jetty, Jackson)
- **Dependency Injection**: Dagger 2 (compile-time)
- **Database Access**: JDBI 3 SqlObject (type-safe SQL, no ORM)
- **Database**: PostgreSQL 15+
- **Migrations**: Flyway (versioned SQL, run at boot)
- **Authentication**: OPAQUE (RFC 9807) via the Hofmann library — zero-knowledge password authentication; the server never sees plaintext passwords. JWT bearer tokens issued on successful login, delivered in HttpOnly cookies.
- **Sessions**: JDBI-backed session store with server-side revocation
- **Audit Logging**: SLF4J audit loggers on mutation paths

**Pattern Detection**:
- **v1 Algorithm**: Rule-based frequency and temporal analysis — count occurrences, compute inter-arrival intervals, flag cycles (daily/weekly/monthly) with low variance. Runs on-device for free-tier Android and server-side for synced users.
- **v2 Algorithm**: PrefixSpan-based sequence mining for multi-step workflow discovery. Server-only, batch-scheduled.
- **Implementation**: Pure Kotlin on Android; pure Java in the `server-db` module. No TensorFlow Lite in v1 — the bloat isn't justified by a rule-based detector's output.
- **Incremental Learning**: Patterns update as new events arrive; old patterns fade if no longer supported.

**Security**:
- **Transport**: TLS 1.3 via AWS ALB termination (prod); self-signed or HTTP on localhost (dev)
- **At Rest**: AES-256 via Aurora storage encryption and S3 server-side encryption
- **Authentication**: OPAQUE eliminates password-in-transit risk; JWT access tokens are short-lived with refresh rotation
- **Access Control**: Owner-scoped queries enforced in managers; RBAC added in the Business tier
- **Secrets**: AWS Secrets Manager (prod), environment variables (dev). Secrets are never stored in the database. Non-secret runtime configuration lives in the `configuration_values` table.
- **Headers**: Servlet filter sets HSTS, CSP, X-Frame-Options, X-Content-Type-Options on all responses including static assets.

**DevOps**:
- **Version Control**: Git / GitHub
- **Build**: Gradle (Kotlin DSL) multi-module
- **CI/CD**: GitHub Actions (test on PR, build+deploy on merge to main)
- **Containerization**: Docker; images pushed to ECR
- **Observability**: OpenTelemetry instrumentation across backend and Android → AWS CloudWatch (metrics, traces, logs)
- **Dev Environment**: Docker Compose (Postgres + app + webapp) — `docker-compose.yml` in repo root
- **Prod Environment**: AWS ECS Fargate behind ALB, Aurora PostgreSQL (serverless v2), S3 for attachments, Secrets Manager for OPAQUE keys and DB creds, CloudFront for webapp static assets

**Scalability**:
- Stateless backend — ECS Fargate autoscales on CPU/request count
- Aurora PostgreSQL serverless v2 scales ACUs on demand; Aurora Reader endpoint used for heavy read paths (reporting, analytics)
- Attachments in S3 served via CloudFront
- Free tier imposes zero backend load (local-only Android)

---

## Pattern Detection Algorithm

### How does the pattern detection algorithm work in detail?

Motif uses a tiered approach: a lightweight rule-based detector ships in v1 (both on-device Android and server), and a PrefixSpan-based sequence miner follows in v2 once event volume justifies it.

**High-Level Approach**:
Pattern detection analyzes time-ordered events (with subject, tags, timestamp) to surface:
1. Repeated individual events (frequency patterns)
2. Multi-step sequences (workflow patterns, v2)
3. Temporal regularities (time-of-day, day-of-week preferences)

**Step-by-Step Process**:

**1. Data Preprocessing**:
- Events are grouped by owner and subject, time-ordered
- Outliers (single occurrences with no follow-up in N days) are excluded from frequency analysis
- Timestamps discretized to hour / day-of-week / day-of-month for cycle detection

**2. Frequency Analysis (v1, rule-based)**:
- For each (owner, subject, event-type) group with ≥3 occurrences, compute inter-arrival intervals (mean, variance, coefficient of variation)
- Low CV (<0.3) → high-confidence periodic pattern
- Classify period: daily (~1d), weekly (~7d), monthly (~30d), other
- Significance threshold: at least 3 occurrences AND coverage of at least 2 complete cycles

**3. Sequence Mining (v2, PrefixSpan)**:
- Extract event sequences within project or time-window boundaries
- Find frequent subsequences with min support ≥ 3
- Max gap between steps configurable (default 14 days)
- Run as a scheduled batch job, not synchronously

**4. Temporal Pattern Detection**:
- Analyze hour-of-day and day-of-week distributions per event type
- Chi-squared test against uniform distribution — non-uniform implies a temporal preference
- Surface as "you usually do X on weekday mornings"

**5. Scoring and Ranking**:
- Score = f(occurrences, consistency, recency, coverage)
- More occurrences, lower variance, more recent, more cycle-coverage → higher score
- Patterns below a minimum score threshold are not surfaced

**6. Pattern Presentation**:
- Top N patterns (typically 3–5) surfaced per dashboard view
- Natural-language templates: "You tend to {event} every {interval}"
- Suggested actions: "Next {event} expected around {date}"

**7. Incremental Learning**:
- Patterns re-evaluated on a schedule (nightly server-side, on-demand on Android)
- Stale patterns (no reinforcing events in 2+ expected cycles) are demoted

**On-Device vs. Cloud**:
- **Free tier (Android)**: Pure Kotlin rule-based detector runs on-device in a WorkManager periodic task. No network, no cloud compute, no ML framework dependency.
- **Premium/Business (server)**: Same rule-based logic in pure Java in the `server-db` module, run as a Dropwizard managed task per owner. Sequence mining (v2) and cross-owner organizational patterns (Business tier) are server-only.

**Privacy Preservation**:
- Pattern detection uses only the owner's own data by default
- No cross-owner training; no PII in pattern outputs
- Business-tier aggregated patterns are computed over an organization's members and visible only to that organization

**Example**:
User logs events on a "Jade Plant" subject: `Watered` (Mar 1), `Watered` (Mar 8), `Watered` (Mar 15), `Watered` (Mar 22). CV on interval = 0 → high confidence weekly pattern. Surfaces: "You tend to water Jade Plant every 7 days — next expected Mar 29."

---

## Data Architecture

### How is data structured and stored?

Two physical schemas, one logical model.

**Android (Free Tier, Local-Only) — Room/SQLite**:

Room schema versioned independently of the server's Flyway migrations. Mirrors the local-first entities:

```
Tables:
- subjects
  - id (UUID, primary key)
  - category_id (FK → categories)
  - value (text)
  - created_at, updated_at
  - sync_state (enum: LOCAL_ONLY, PENDING_UPLOAD, SYNCED) -- post-signup

- events
  - id (UUID, primary key)
  - subject_id (FK → subjects)
  - value (text)
  - timestamp (datetime)
  - sync_state

- notes
  - id (UUID, primary key)
  - subject_id (FK → subjects, nullable)
  - event_id (FK → events, nullable)
  - value (text)
  - timestamp (datetime)
  - sync_state

- categories
  - id (UUID, primary key)
  - name (text), color (text), icon (text)

- tags
  - id (UUID, primary key)
  - value (text)

- entity_tags
  - entity_id, entity_type, tag_id

- patterns (detected locally, not synced)
  - id, type, description, confidence_score
  - last_detected_at, occurrences, metadata (json)
```

UUIDs are generated locally so the same identifier is stable across the local DB and the server post-sync.

**Backend (Premium/Business) — PostgreSQL**:

Multi-tenant with per-owner isolation enforced at the manager layer. Schema evolves via Flyway migrations in the `server-db` module.

```
Core tables:
- owners (id, value, tier, organization_id nullable, deleted, timestamps)
- categories (id, owner_id, name, color, icon)
- subjects (id, owner_id, category_id, project_id nullable, value, timestamps)
- events (id, owner_id, subject_id, value, timestamp, timestamps)
- notes (id, owner_id, subject_id, event_id, value, timestamp)
- tags (id, owner_id, value)
- entity_tags (entity_id, entity_type, tag_id)

Premium tables:
- projects (id, owner_id, name, description, status, timestamps)
- workflows (id, owner_id, name, description, timestamps)
- workflow_steps (id, workflow_id, position, name, expected_duration_ms, notes)
- attachments (id, owner_id, entity_id, entity_type, storage_key, size, mime_type)
- patterns (id, owner_id, type, description, confidence, metadata jsonb, last_detected_at)

Business tables:
- organizations (id, name, billing_customer_id, plan, timestamps)
- organization_members (id, organization_id, owner_id, role, joined_at)
- audit_log (id, owner_id, organization_id, entity_type, entity_id, action, metadata jsonb, at)

Auth tables (existing, Hofmann-managed):
- opaque_credentials, opaque_sessions, opaque_pending_sessions

Subscription tables:
- subscriptions (id, owner_id or organization_id, stripe_customer_id, stripe_subscription_id,
                 status, current_period_end, ...)
```

All user-data tables carry `owner_id` and cascade-delete from `owners`. Multi-owner entities (Business tier) also carry `organization_id` and a `visibility` enum (PRIVATE, TEAM, ORG).

**Access Control Enforcement**:
- JAX-RS authentication filter resolves the OPAQUE session to an `Owner` and attaches memberships to the request context
- Every manager method takes `Owner` (or `RequestContext` in Business tier) and scopes queries accordingly
- Database-level enforcement is considered for Business tier via Postgres Row-Level Security as a defense-in-depth measure

---

## Offline Functionality

### How do you handle offline functionality?

Offline-first on Android, connectivity-required on web.

**Free Tier (Android, Local-Only)**:
- 100% local via Room. No network calls, ever.
- Event logging is a pure local write — latency is disk-bound, not network-bound.
- Pattern detection runs on-device via WorkManager.
- No sync, no account, no network dependency.

**Premium/Business (Android Synced Mode)**:
- **Offline Writes**: Events/subjects/notes written to Room first with `sync_state = PENDING_UPLOAD`
- **Offline Reads**: UI always reads from Room — no spinner waiting for the network
- **Background Sync**: WorkManager periodic task uploads pending writes, downloads server changes since last sync watermark
- **Conflict Resolution**: **Last-write-wins** on a per-entity basis, keyed by `updated_at`. Chosen for simplicity; adequate given the single-user-per-owner model. Multi-user merging is scoped to the Business tier and handled via server-side visibility rules rather than client-side CRDTs.
- **Deletes**: Soft-delete locally (tombstone row), hard-delete on successful sync

**Webapp (Online-Only)**:
- The webapp is an authenticated SPA that assumes connectivity. No offline mode.
- Brief disconnections surface as retry toasts; no local queue.

**User Experience**:
- No "you're offline" blockers on Android
- Small sync-state indicator (pending badge) when uploads are queued
- Manual "force sync" in settings for troubleshooting

**Edge Cases**:
- Large attachments: queued for upload, uploaded only on Wi-Fi by default (user-configurable)
- Team workflows (Business): downloaded to device when accessed; LWW conflict resolution means late-arriving edits overwrite earlier ones — acceptable for a v1 given workflow edits are infrequent

**Testing**:
- Android sync engine gets a dedicated test harness with simulated network partitions, clock skew, and interrupted uploads
- Backend sync endpoints tested with Testcontainers + fake clock

---

## Data Migration and Export

### What about data migration and export?

**Export** (all tiers):
- Users can export complete data in JSON or CSV
- Export covers: subjects, events, notes, categories, tags, projects, workflows, patterns, attachment metadata (blobs downloaded separately)
- One-click in settings; server generates a signed S3 URL for Premium/Business; Android writes a file to the Downloads folder for free tier
- Premium/Business can schedule automated exports

**Import**:
- CSV import for events (useful for spreadsheet migration)
- JSON import for full-data restore (matches export format — round-trip safe)
- Future: importers for competitor tools (Todoist, Asana)

**Android → Cloud Migration (Signup)**:
- On signup, OPAQUE registration completes and creates an `Owner` on the server
- Android uploads all local entities in batches (subjects first, then events, then notes), preserving local UUIDs
- Server inserts with the supplied UUIDs; conflicts are impossible because the server has no prior data for this owner
- Post-upload, Android transitions to synced mode; `sync_state` on all rows flips to `SYNCED`

**Account Deletion**:
- User-initiated from settings (webapp or Android)
- 30-day grace period (data retained but inaccessible; account marked `deleted = true`)
- After 30 days: hard-delete via scheduled job. Cascade deletes clear all user data; S3 attachments deleted via lifecycle policy.
- Export offered before deletion finalizes

**Data Portability (GDPR compliance)**:
- Download all data in machine-readable JSON
- No vendor lock-in — users own their data
- API endpoint for programmatic export (Business tier)

**Business Tier Considerations**:
- Organization admins can export org-wide data (subject to permissions)
- Individual users can export personal data even while employed (separates personal from organizational data)
- When an employee leaves, personal subjects/events stay with the person; org-visibility entities stay with the organization

---

## Infrastructure Scaling

### How do you plan to scale the infrastructure?

**Current Architecture** (Years 1–2):
- **Dev**: Docker Compose on a developer laptop (Postgres + Dropwizard + Vite dev server)
- **Prod**: AWS — ECS Fargate (stateless Dropwizard containers behind ALB), Aurora PostgreSQL (serverless v2) with a multi-AZ writer and one or more reader instances, S3 for attachments, CloudFront for webapp static assets, Secrets Manager for OPAQUE keys and DB credentials
- **Free tier**: Zero backend load — all compute is on-device Android

**Scaling Path**:

1. **ECS Fargate**: scale on CPU and request-rate target-tracking. Task count grows with load; no per-instance state.
2. **Aurora PostgreSQL**: serverless v2 autoscales ACUs within a configured min/max range. Writer handles all mutations; readers (auto-added within the cluster) serve reporting and analytics queries via the Aurora Reader endpoint. Write capacity rarely the bottleneck given event-log throughput per user.
3. **S3 + CloudFront**: inherently scalable; cost-managed via lifecycle policies (move cold attachments to Infrequent Access tier after 90 days).
4. **Pattern detection**: runs per-owner as a managed task; if single-owner pattern jobs become expensive, move to an SQS-fed worker fleet.

**Scaling Bottlenecks to Monitor**:
1. **DB connections**: JDBI pool size × ECS task count must stay within Aurora `max_connections` (Aurora limits scale with ACU/instance size). Add RDS Proxy in front of the cluster if the product needs more tasks than the cluster can handle directly.
2. **Pattern detection latency**: nightly per-owner scan becomes O(owners × events); rewrite as incremental once scale demands.
3. **Attachment storage**: S3 is cheap but transfer costs matter. CloudFront in front of S3 amortizes this.

**Optimizations**:
- **Query optimization**: index all `(owner_id, ...)` access patterns; `EXPLAIN ANALYZE` on hot queries in CI
- **Caching**: application-level caching for pattern results (short TTL); HTTP caching with ETags for stable resources
- **Batching**: pattern detection and sync reconciliation batch-process rather than per-event
- **Archiving**: events older than 2 years move to a `events_archive` partition; still queryable but colder-storage

**Business Tier Scaling (Multi-Tenant)**:
- Logical separation: all queries filtered by `organization_id` in addition to `owner_id`
- Postgres Row-Level Security as a defense-in-depth option for Business tier
- Very large organizations (>1M events) may require table partitioning by `organization_id` or by time

**Cost Management**:
- AWS Cost Explorer + budget alerts
- Aurora reserved capacity once steady-state ACU usage is understood
- Fargate Spot for non-critical workloads (pattern detection batch jobs)

**Disaster Recovery**:
- Aurora continuous backup to S3 with 35-day point-in-time recovery; cross-region snapshot copy for Business tier
- S3 versioning enabled; MFA-delete on production buckets
- Infrastructure-as-Code (Terraform) so environments are reproducible
- Documented runbook for region failover

---

## Security Architecture

### What are the security measures?

**Authentication**:
- **OPAQUE (RFC 9807)** via the Hofmann library — zero-knowledge password authentication. The server never receives or stores plaintext passwords, even momentarily. This is stronger than bcrypt/scrypt/argon2 at rest because there's nothing password-equivalent to steal.
- **Client-side Argon2** (parameters: 64MiB memory, 3 iterations, 4 parallelism in prod) stretches the password before the OPAQUE handshake
- **JWT access tokens** with 1-hour expiration, issued on successful OPAQUE login
- **Refresh tokens** with 30-day expiration and rotation-on-use; server-side session revocation supported
- **Password requirements**: minimum 12 characters (Business tier enforces stronger policy)
- **Account recovery**: via verified email with one-time token; recovery does *not* decrypt prior data because of OPAQUE — recovery means re-registration, with user data preserved under the owner identity

**Session Management**:
- JWT delivered in an `HttpOnly; Secure; SameSite=Strict` cookie (never readable by JavaScript)
- CSRF protection via double-submit token for state-changing requests
- Session cleanup: scheduled task purges expired `opaque_sessions` rows

**Data Encryption**:
- **In Transit**: TLS 1.3 enforced at the ALB; HTTP redirected to HTTPS; HSTS header with preload
- **At Rest**: AES-256 via Aurora storage encryption and S3 server-side encryption (SSE-S3 or SSE-KMS for business-tier data)
- **Secrets**: AWS Secrets Manager. OPAQUE server keys (`serverKeySeedHex`, `oprfSeedHex`) and JWT signing secret loaded at boot, rotated on a documented schedule. **Secrets are never stored in the database.** The `configuration_values` table is retained for non-secret runtime configuration (feature flags, tunable thresholds, default values) and is subject to code review to ensure nothing sensitive is added.

**Access Control**:
- **Owner scoping**: every manager method filters by `owner_id`; no implicit global queries
- **RBAC (Business tier)**: Owner / Admin / Member roles with capability matrix enforced at the resource layer and re-checked in managers
- **Visibility (Business tier)**: PRIVATE / TEAM / ORG flags on entities; resolved at query construction
- **Principle of least privilege** for service accounts (ECS task roles, Aurora IAM auth where feasible)
- **Row-Level Security** considered for Business tier as defense-in-depth

**API Security**:
- **Rate limiting**: sliding-window limits on auth endpoints and subscription webhooks; per-owner limits on write endpoints
- **Input validation**: every resource endpoint validates and returns 400 on malformed input (no 500s on bad input)
- **CORS**: restricted to the webapp origin in prod
- **Security headers**: servlet filter sets HSTS, CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy on all responses including static assets

**Code Security**:
- **Dependency scanning**: Dependabot on GitHub; `./gradlew dependencyCheck` in CI
- **Static analysis**: Error Prone (Java), ESLint (TypeScript), Detekt (Kotlin)
- **No hardcoded secrets**: pre-commit hook + CI scanner (gitleaks)
- **Security reviews**: `/security-review` before each tier launch; external penetration test annually

**Privacy Measures**:
- **Free tier**: fully local — no data collection, no analytics phone-home
- **Premium/Business**: minimal data collection, no third-party sharing, opt-in anonymous analytics
- **GDPR/CCPA compliance**: data access, deletion, portability from day one

**Incident Response**:
- **Monitoring**: OpenTelemetry → CloudWatch Logs, Metrics, X-Ray traces; alerts on error-rate spikes, auth-failure spikes, and latency regressions
- **Runbooks**: documented incident response procedures, on-call rotation
- **Breach notification**: within 72 hours per GDPR
- **Insurance**: cyber liability coverage

**Compliance**:
- **GDPR / CCPA** from launch
- **SOC 2 Type II** targeted for Year 2
- **Bug bounty** program targeted for Year 2

---

## Development Workflow

### What is the development process?

**Development Environment**:
- **IDE**: IntelliJ IDEA (backend, Android), VS Code (webapp)
- **Version Control**: Git with GitHub
- **Branching Strategy**: Trunk-based with short-lived feature branches; `main` is always deployable
- **Code Review**: all changes require PR review before merge; CI must pass
- **Local dev**: `docker-compose up` brings up Postgres + backend + webapp hot-reload

**Build**:
- **Gradle Kotlin DSL** multi-module (`server`, `server-db`, `common`, `webapp`, `android` — Android added in Phase 5)
- **Reproducible builds** via locked dependency versions
- `./gradlew build` runs the full backend test suite and webapp build

**Testing Strategy**:
- **Unit tests**: JUnit 5 + AssertJ for business logic (target: 80% coverage on managers)
- **DAO/manager integration tests**: Testcontainers PostgreSQL with Flyway migrations applied (real database, not mocks — see project feedback on this)
- **Resource tests**: Dropwizard `ResourceExtension` exercising JAX-RS endpoints
- **Webapp unit tests**: Vitest
- **E2E tests**: Playwright against a Docker Compose stack (added in Phase 1)
- **Android unit tests**: JUnit + Robolectric
- **Android UI tests**: Espresso / Compose UI test
- **Manual QA checklist**: maintained per release

**CI/CD Pipeline** (GitHub Actions):
- **On PR**: lint, unit tests, build verification, dependency-vulnerability scan
- **On merge to main**: full test suite, build Docker image, push to ECR, deploy to staging ECS
- **On release tag**: deploy to production ECS with canary (10% → 50% → 100%)
- **Android**: built on tag, uploaded to Google Play internal testing track

**Release Process**:
1. Feature development on feature branches
2. PR review + CI green
3. Merge to main → auto-deploy to staging
4. Internal testing against staging environment
5. Tag release (`vX.Y.Z`) → canary rollout to production
6. Monitor OpenTelemetry metrics and error rates
7. Full rollout if canary healthy; automatic rollback on alert threshold breach

**Quality Targets**:
- Backend request p95 latency: <200ms
- Android event logging: <500ms (local write)
- Android app startup: <2s cold, <500ms warm
- Backend crash rate: <0.01% of requests
- Android crash rate: <0.1% of sessions (Crashlytics)
- App store rating target: 4.5+

**Performance Monitoring**:
- **OpenTelemetry** instrumentation on all HTTP handlers, DAO calls, and Android user flows
- **CloudWatch Metrics**: request rate, error rate, p50/p95/p99 latency by endpoint
- **CloudWatch X-Ray**: distributed tracing across ALB → ECS → Aurora
- **Custom business metrics**: events logged per day, signups, pattern detections, tier transitions

---

## Technical Risks

### What are the key technical risks?

**Risk 1: Pattern Detection Accuracy**
- **Problem**: Detector might surface irrelevant or incorrect patterns, frustrating users
- **Mitigation**: Start with a transparent rule-based detector (explainable to users), validate with beta feedback, allow users to dismiss patterns (negative feedback signal), gradually tune thresholds
- **Fallback**: Manual workflow creation is always available — pattern detection is additive

**Risk 2: OPAQUE on Android** (resolved)
- **Original concern**: Hofmann was assumed to be JavaScript-only, which would have forced either a Kotlin port, a WebView workaround, or a JWT-only fallback on mobile
- **Resolution**: the hofmann-elimination project ships `hofmann-client`, a Java client library that runs on the Android JVM runtime directly. No port, no WebView, no fallback — the same OPAQUE security guarantees hold on Android as on the web.
- **Residual risk**: `hofmann-client` version compatibility with the server-side Hofmann version must be tracked; integration tests in the Android module should exercise a live handshake against a test backend

**Risk 3: Offline Sync Conflicts**
- **Problem**: Multi-device editing with LWW can lose writes under unlucky timing
- **Mitigation**: LWW is explicitly chosen for simplicity at v1 scale (single user per owner). Mitigated by sync running frequently when online. Multi-user merging scoped to Business tier and handled via server-side visibility rather than client-side CRDTs.
- **Testing**: sync test harness simulates partition/clock-skew scenarios

**Risk 4: Operational Complexity**
- **Problem**: Self-hosting the backend (rather than Firebase) means owning uptime, backups, security patches
- **Mitigation**: Managed AWS services (ECS Fargate, Aurora, Secrets Manager) eliminate most host-level operations. Infrastructure-as-Code (Terraform) makes environments reproducible. On-call rotation from Month 12.
- **Threshold**: if operational toil exceeds 20% of engineering time, re-evaluate managed-PaaS options

**Risk 5: Scalability Costs**
- **Problem**: AWS costs could grow faster than revenue
- **Mitigation**: Free tier has *zero* backend cost by design. Paid tiers only pay for themselves if unit economics are healthy (LTV:CAC targets in `business_plan.md`). Reserved Instances, Fargate Spot, S3 lifecycle policies applied once steady-state load is known.
- **Threshold**: if AWS costs exceed 20% of paid-tier revenue, trigger optimization sprint

**Risk 6: Security Breach**
- **Problem**: Breach destroys trust and triggers regulatory penalties
- **Mitigation**: OPAQUE makes password breach effectively impossible (nothing stealable). Encryption at rest and in transit. Secrets in AWS Secrets Manager, never in DB. Annual pen tests. Bug bounty by Year 2. Cyber liability insurance.
- **Response**: 72-hour breach notification per GDPR; incident response runbook; customer comms plan

**Risk 7: Third-Party Dependency Failures**
- **Problem**: AWS outage, Hofmann library regression, Stripe outage
- **Mitigation**: AWS multi-AZ by default; cross-region disaster recovery for Business tier. Hofmann is vendored and versioned deliberately. Stripe outage → webhooks retry automatically; subscription state reconciled on next successful webhook or nightly reconciliation job.
- **Graceful degradation**: free-tier Android unaffected by any backend outage

**Risk 8: Vendor Lock-in to AWS**
- **Problem**: Migration away from AWS would be costly
- **Mitigation**: Keep the stack portable — Postgres (not DynamoDB), S3-compatible abstraction for attachments, OpenTelemetry (vendor-neutral). No Lambda, no DynamoDB, no AWS-proprietary APIs in application code.
- **Contingency**: the application can run on any container platform with any Postgres + object store; rehosting is weeks, not months

---

## Technology Stack Summary

### Complete Technology Stack

**Backend**:
- Java 17 (LTS)
- Dropwizard 4 (Jetty, Jersey JAX-RS, Jackson)
- JDBI 3 SqlObject
- PostgreSQL 15+
- Flyway
- Dagger 2
- Hofmann server library (OPAQUE RFC 9807)
- SLF4J + Logback

**Web**:
- Vite
- React 19
- TypeScript
- React Router 7
- Tailwind CSS
- Radix UI
- `hofmann-js` (web OPAQUE client)

**Android** (Phase 5):
- Kotlin
- Jetpack Compose
- Room
- Dagger 2
- WorkManager
- Coroutines & Flow
- `hofmann-client` (Java OPAQUE client)

**Infrastructure**:
- AWS ECS Fargate (compute)
- AWS Aurora PostgreSQL serverless v2 (database)
- AWS S3 (attachments, backups)
- AWS CloudFront (webapp CDN)
- AWS Application Load Balancer (ingress, TLS termination)
- AWS Secrets Manager (keys and credentials)
- AWS ECR (container registry)
- Docker (dev and prod)
- Terraform (infrastructure as code)

**Observability**:
- OpenTelemetry (instrumentation)
- AWS CloudWatch Logs
- AWS CloudWatch Metrics
- AWS X-Ray (distributed tracing)

**DevOps**:
- GitHub Actions (CI/CD)
- Dependabot (dependency updates)
- Gitleaks (secret scanning)

**Development Tools**:
- IntelliJ IDEA
- VS Code (webapp)
- Android Studio (Phase 5)
- Git / GitHub
- Docker Compose (local env)

**Testing**:
- JUnit 5 + AssertJ (backend, Android)
- Testcontainers (backend integration)
- Vitest (webapp unit)
- Playwright (webapp E2E)
- Espresso / Compose UI test (Android UI)
- Robolectric (Android unit)

---

## Technical Roadmap Priorities

### Development Priorities

Phase numbers match `docs/migration_plan.md`.

**Phase 0 — Foundation corrections** (1–2 weeks)
- Rewrite technical architecture doc (this document)
- Move secrets (OPAQUE keys, JWT signing secret) out of `configuration_values` to env vars / Secrets Manager; retain `configuration_values` for non-secret runtime config
- GitHub Actions CI pipeline
- Terraform scaffolding for staging environment

**Phase 1 — Category & domain polish** (2 weeks)
- Promote `Category` to first-class entity (color, icon)
- Flyway migrations V12–V14 with backfill
- Default categories seeded on owner creation
- E2E test harness (Playwright)

**Phase 2 — Pattern detection MVP** (3–4 weeks)
- `Pattern` entity, DAO, manager
- Server-side rule-based frequency detector
- Nightly Dropwizard managed task
- `GET /api/patterns` resource
- Webapp dashboard pattern surface

**Phase 3 — Premium tier** (5–7 weeks)
- `Project` entity
- `Workflow` + `WorkflowStep`
- Pattern → Workflow conversion
- Attachments (S3-backed, dev filesystem impl)
- Stripe billing
- Tier enforcement

**Phase 4 — Business tier** (6–8 weeks)
- Authorization test harness (before any refactor)
- `Organization` + `OrganizationMember`
- Visibility + RBAC refactor across managers
- Audit log
- Admin console webapp
- Team analytics

**Phase 5 — Android app** (parallel track, Months 3–9)
- Free-tier Android: Room + Compose + on-device pattern detector, no network
- Signup + OPAQUE via `hofmann-client`
- Sync engine: WorkManager + LWW conflict resolution
- Premium features gated on tier

**Phase 6 — Scale & Expand** (Month 12+)
- Public API for Business tier
- Slack / Zapier integrations
- iOS app
- Internationalization (es, fr, de)
- Enterprise SSO (SAML / OIDC)
- Workflow marketplace

---

## Conclusion

The technical architecture for Motif is designed for:
- **Privacy**: free tier is truly local — no backend contact, no analytics, no account
- **Security**: OPAQUE authentication means the server can't leak what it doesn't have
- **Portability**: Postgres + S3 + OpenTelemetry over AWS-proprietary services so the stack isn't welded to a vendor
- **Progressive disclosure**: the same Android binary serves free users locally and synced users through the cloud — signup is an upgrade, not a rebuild
- **Operational simplicity**: managed AWS services (ECS Fargate, Aurora, Secrets Manager) minimize host-level ops while remaining standards-based

By leveraging a proven server stack (Dropwizard + Postgres), a modern cross-platform webapp (React + Vite), a purpose-built Android client, and rigorous pattern detection that starts simple and grows sophisticated, Motif can deliver on its promise: turning what you do into what you should do next — without compromising on privacy, security, or user ownership of their data.

The technical foundation is solid, the risks are identified and mitigated, and the roadmap is clear.
