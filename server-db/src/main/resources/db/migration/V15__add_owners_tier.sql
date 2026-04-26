-- Adds the tier column to owners. New owners default to FREE_SYNCED — the owner row
-- is only ever created when a user signs up for cloud sync, so there is no FREE
-- (local-only) tier represented here. Promotion to PREMIUM/BUSINESS happens via the
-- admin endpoint (Phase 3a) or, eventually, via Stripe webhook (Phase 3e).
ALTER TABLE owners
    ADD COLUMN tier VARCHAR(16) NOT NULL DEFAULT 'FREE_SYNCED';

-- Defense in depth: reject typos at the database level, not just in app code.
ALTER TABLE owners
    ADD CONSTRAINT owners_tier_check
    CHECK (tier IN ('FREE_SYNCED', 'PREMIUM', 'BUSINESS'));
