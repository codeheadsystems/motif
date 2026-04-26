# Motif Migration Plan

How the current codebase evolves to meet the product described in `business_plan.md` and `features_overview.md`.

---

## Strategic decisions (locked in)

1. **Backend stack stays as-is.** Dropwizard 4 + JDBI + PostgreSQL + Flyway + Dagger, with OPAQUE (Hofmann) auth. The `technical_architecture.md` doc will be rewritten to reflect this — Firebase is *not* the target.
2. **Web app stays.** React/Vite SPA is the Premium/Business cross-platform client.
3. **Android app is built as a parallel track.** Android is the free-tier entry point: local Room/SQLite, no account required, fully offline, zero backend contact. Signing up upgrades the same Android app to cloud-synced mode *and* unlocks the webapp.
4. **Domain model keeps Subject as primary.** Subject = the thing being tracked (a plant, a room, a client). Event = something that happened to it (watered, painted, called). Note = freeform text attached to a subject or event. No renames.

---

## Domain model — target shape

Extensions to the current model, not rewrites:

| Entity | Status | Notes |
|---|---|---|
| `Owner` | exists | Add `tier` (FREE_SYNCED / PREMIUM / BUSINESS), `organization_id` nullable. FREE_SYNCED means "has account, no paid plan" — the free Android user who signed up for cloud sync. Pure-local free users have no `Owner` row. |
| `Subject` | exists | Promote `category` from string to FK → `Category`. Add optional `project_id` FK (Premium). |
| `Event` | exists | Current shape (owner, subject, value, timestamp) is correct. |
| `Note` | exists | Current shape is correct. |
| `Tag` | exists | Current shape is correct. |
| `Category` | **new first-class entity** | `id`, `owner_id`, `name`, `color`, `icon`. Seed per-owner defaults on signup (Home, Health, Creative, Learning, Professional). Currently a string on `Subject`. |
| `Project` | **new** (Premium) | `id`, `owner_id`, `name`, `description`, `status` (ACTIVE/PAUSED/COMPLETED/ARCHIVED), timestamps. Subjects optionally belong to a Project. |
| `Workflow` + `WorkflowStep` | **new** (Premium) | Workflow is an ordered sequence of named steps with expected durations. Generated from detected Patterns or hand-authored. |
| `Pattern` | **new** | Detected patterns cache: `type` (FREQUENCY/SEQUENCE/TEMPORAL), `description`, `confidence`, `metadata` (jsonb), `last_detected_at`. Output of `PatternDetectionManager`. |
| `Organization` + `OrganizationMember` | **new** (Business) | Member role: OWNER/ADMIN/MEMBER. Most Premium entities grow an optional `organization_id` and a `visibility` flag (PRIVATE / TEAM / ORG). |
| `AuditLog` | **new** (Business) | Append-only record of mutations for compliance. |
| `Attachment` | **new** (Premium) | Blob metadata row + object-store key. |

---

## Free-tier (Android, no account)

The free tier never hits the Dropwizard backend. Two design consequences:

- **Android has its own Room schema.** It mirrors the local-first entities (Subject, Event, Note, Tag, Category, Pattern) but is independent of the server's Flyway migrations. Versioned separately.
- **Pattern detection ships on-device.** Start with a simple rule-based frequency detector in pure Kotlin; upgrade to TensorFlow Lite only if the rule-based version is insufficient. The cloud pattern detector for Premium is a *separate* implementation in the server (see Phase 3).

**Signup flow**: user hits "Sync to cloud" → OPAQUE registration via Hofmann → server creates `Owner` row (tier=FREE_SYNCED) → Android uploads local data in batches → thereafter, Android writes locally and syncs in background. No conflict resolution on the first upload (server is empty); ongoing sync uses last-write-wins per entity, with workflow edits flagged for manual merge (per existing tech-arch doc).

---

## Phased plan

### Phase 0 — Foundation corrections (1–2 weeks)

Non-negotiable prerequisites before building new features.

- **Rewrite `docs/technical_architecture.md`** to describe the actual stack (Dropwizard, JDBI, Postgres, OPAQUE, React) with Android as a parallel client. Keep `business_plan.md` and `features_overview.md` as-is.
- **Secrets out of the database.** `configuration_values` currently stores `serverKeySeedHex`, `oprfSeedHex`, `jwtSecretHex` in plaintext. Move those three to env vars (dev) / AWS Secrets Manager (prod). The `configuration_values` table is retained for non-secret runtime config (feature flags, tunable thresholds, defaults). TODO.md flags the secrets issue as a production blocker.
- **HTTPS / TLS termination plan.** Decide: reverse proxy (nginx/Caddy) vs. Dropwizard native TLS. Document.
- **CI/CD**: confirm/add GitHub Actions workflow running `./gradlew build` and the webapp build on PRs. Subagent didn't find one.

### Phase 1 — Category & domain polish — **COMPLETE** (2026-04-25)

Small, high-leverage changes that make later phases natural.

- ✅ Promoted `Category` from string-on-Subject to first-class entity with `color` and `icon`. Single atomic Flyway migration `V13__promote_category_to_entity.sql` creates the table, backfills from existing `subject.category` strings (gray + tag default), adds `subjects.category_uuid` FK, and drops the old column.
- ✅ Default categories (Home, Health, Creative, Learning, Professional) seeded on owner creation via `OwnerResolver` calling `CategoryManager.seedDefaults`. Idempotent.
- ✅ Updated `Subject` model (`Identifier categoryIdentifier`), `SubjectDao`, `SubjectManager`, `SubjectResource`, `EventDao` row-mapper.
- ✅ New `CategoryResource` at `/api/categories` with full CRUD; delete refuses with 409 when subjects reference the category.
- ✅ Removed legacy `GET /api/subjects/categories` endpoint.
- ✅ Webapp: `lucide-react` icon mapping, route changed `/c/:category` → `/c/:categoryId`, sidebar shows colored dots + icons, full TypeScript clean.
- ✅ Tests: `CategoryManagerIntegrationTest`, expanded `CategoryTest`, all existing DAO/manager/resource tests updated for FK semantics. Backend + webapp + CDK tests all green.

### Phase 2 — Pattern detection MVP (3–4 weeks)

Server-side only, for synced users. On-device Android detector is built in the Android track.

- `Pattern` entity, DAO, manager. Migration V15.
- `PatternDetectionManager` with a frequency-only detector v1: for each owner, compute per-(subject, event-type) occurrence intervals, flag patterns with ≥3 occurrences and low variance. PrefixSpan (sequence mining) is v2.
- Dropwizard managed task running nightly per owner.
- `GET /api/patterns` resource returning top N.
- Webapp: "Discovered patterns" card on dashboard (`DashboardHome`).

### Phase 3 — Premium tier (5–7 weeks)

Features gated behind `Owner.tier = PREMIUM`.

- `Project` entity + Subject FK. DAO, manager, resource, webapp routes (`/p/:projectId`), project dashboard component.
- `Workflow` + `WorkflowStep` entities. Manual authoring UI in webapp. `POST /api/workflows/from-pattern/:patternId` to convert a detected Pattern into an editable Workflow.
- `Attachment` abstraction: `AttachmentStore` interface with filesystem impl (dev) and S3-compatible impl (prod). 10MB per-file cap enforced at resource boundary.
- **Billing**: Stripe integration (webapp-first; Android can use Google Play billing later). New `subscriptions` table, Stripe webhook handler, tier transitions.
- **Tier enforcement** as a cross-cutting concern: add a `@RequiresTier(PREMIUM)` JAX-RS filter, or enforce in managers. Prefer manager-level for defense in depth.

### Phase 4 — Business tier (6–8 weeks)

The highest-risk phase because it touches ownership checks everywhere.

- **Authorization test harness first.** Before any refactoring, add a test suite that enumerates every resource endpoint × every role × every visibility combination and asserts the expected allow/deny. Without this, RBAC changes will introduce IDOR-class bugs (see TODO.md's fixed-but-recurring pattern).
- `Organization`, `OrganizationMember` entities. Migrations add optional `organization_id` and `visibility` to Subject, Project, Workflow, Event, Note.
- Refactor every manager to consult `(owner, organization_membership, visibility)` tuple instead of just `owner`. The current `@Auth HofmannPrincipal` pattern becomes a `RequestContext` with owner + org memberships resolved at the filter.
- `AuditLog` table + append-only logger invoked from managers on mutations.
- Admin console in webapp (user management, billing portal, audit log viewer).
- Team analytics endpoints.

### Phase 5 — Android app (parallel track, starts Month 3, lands ~Month 9)

Runs in parallel with backend phases 2–4. Doesn't block the webapp roadmap.

- Kotlin + Jetpack Compose. Room for local storage. Dagger 2 for DI (matches server for team consistency).
- **Free-tier first**: local-only Subject/Event/Note/Category/Tag/Pattern, on-device frequency detector. Ship this to Play Store before starting sync work.
- **Sync engine**: WorkManager-backed background sync, OPAQUE login via `hofmann-client` (the Java client library from the hofmann-elimination project) — runs natively on the Android JVM, no port or WebView needed.
- **Signup flow**: in-app OPAQUE registration → bulk upload of local entities → mark local DB as synced → thereafter incremental sync.
- Premium features (Projects, Workflows, attachments) require signup and appear conditionally.

### Phase 6 — Scale (Month 12+)

Per `business_plan.md`: API access, Slack/Zapier integrations, iOS app, internationalization, enterprise SSO. All additive on top of the Phase 4 architecture.

---

## Cross-cutting concerns (work throughout)

- **Tests**: webapp has ~1 test file. E2E via Playwright is the biggest coverage gap; add in Phase 1.
- **Observability**: structured logging + a metrics endpoint (Micrometer/Prometheus) before Phase 3. Needed once billing is live.
- **Data export**: `business_plan.md` commits to JSON/CSV export for GDPR. Build once, in Phase 3, covering all entities.
- **Rate limiting**: on login + signup + Stripe webhook at minimum, before Premium launch.
- **Security review cadence**: TODO.md is from 2026-04-05. Re-run at end of each phase; re-run `/security-review` before Premium and Business launches.

---

## What *not* to build

- Don't reimplement OPAQUE on the backend — Hofmann is doing it and it's already integrated.
- Don't add a local-first mode to the webapp. Free tier lives on Android; webapp is paid-only. Keeps scope honest.
- Don't build federated learning / cross-user pattern training. The `business_plan.md` mentions it as an option; skip until there's product-market fit data justifying the complexity.
- Don't port to Firebase. The `technical_architecture.md` doc mentioned it; the rewrite in Phase 0 removes it.

---

## Open questions

1. **Pattern detection**: rule-based Kotlin v1 on-device is cheap. Is TF Lite ever worth the 15+MB APK bloat for the free tier? Defer until v1 proves insufficient.
2. **Business tier visibility model**: three levels (private/team/org) or two (private/org)? Three is richer; two is simpler to enforce correctly. Decision needed at start of Phase 4.
3. **Payment provider for Android signups**: Google Play billing is required by Play Store policy for subscriptions sold inside the app. This means two billing paths (Stripe web, Google Play mobile). Accept the complexity or restrict signup-with-payment to the webapp only.
