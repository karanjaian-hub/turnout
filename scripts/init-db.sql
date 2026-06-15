-- =============================================================================
-- Turnout Database Initialisation
-- Fully idempotent — safe to run multiple times (IF NOT EXISTS everywhere)
-- =============================================================================

-- ── Schemas ──────────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS events;
CREATE SCHEMA IF NOT EXISTS guests;
CREATE SCHEMA IF NOT EXISTS email;
CREATE SCHEMA IF NOT EXISTS payment;

-- ── PostgreSQL ENUMs (mirror Java enums in common-dto) ────────────────────────
DO $$ BEGIN
    CREATE TYPE auth.user_role AS ENUM ('SUPER_ADMIN', 'ADMIN', 'EVENT_ORGANIZER');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE auth.account_status AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE events.event_status AS ENUM ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE guests.rsvp_status AS ENUM ('PENDING', 'CONFIRMED', 'DECLINED', 'MAYBE', 'WAITLISTED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE email.email_status AS ENUM ('PENDING', 'QUEUED', 'SENT', 'FAILED', 'BOUNCED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE payment.plan_type AS ENUM ('FREE', 'PRO', 'ENTERPRISE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE payment.payment_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ── auth.users ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auth.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            auth.user_role NOT NULL DEFAULT 'EVENT_ORGANIZER',
    status          auth.account_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON auth.users(email);
CREATE INDEX IF NOT EXISTS idx_users_role  ON auth.users(role);

-- ── events.events ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS events.events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by          UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    location            VARCHAR(500) NOT NULL,
    event_date          TIMESTAMP NOT NULL,
    max_capacity        INTEGER,
    current_rsvp_count  INTEGER NOT NULL DEFAULT 0,
    status              events.event_status NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_events_created_by ON events.events(created_by);
CREATE INDEX IF NOT EXISTS idx_events_status       ON events.events(status);
CREATE INDEX IF NOT EXISTS idx_events_event_date   ON events.events(event_date);

-- ── events.audit_logs ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS events.audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES events.events(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_id ON events.audit_logs(event_id);

-- ── guests.guests ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS guests.guests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES events.events(id) ON DELETE CASCADE,
    email       VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255) NOT NULL DEFAULT '',
    token       VARCHAR(500) UNIQUE,
    token_used  BOOLEAN NOT NULL DEFAULT FALSE,
    rsvp_status guests.rsvp_status NOT NULL DEFAULT 'PENDING',
    rsvp_date   TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, email)
);

CREATE INDEX IF NOT EXISTS idx_guests_event_id    ON guests.guests(event_id);
CREATE INDEX IF NOT EXISTS idx_guests_email       ON guests.guests(email);
CREATE INDEX IF NOT EXISTS idx_guests_rsvp_status ON guests.guests(rsvp_status);

-- ── email.email_logs ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS email.email_logs (
    id                BIGSERIAL PRIMARY KEY,
    event_type        VARCHAR(255) NOT NULL,
    recipient_email   VARCHAR(255) NOT NULL,
    recipient_name    VARCHAR(255),
    subject           VARCHAR(500) NOT NULL,
    status            VARCHAR(50)  NOT NULL,
    provider_response TEXT,
    attempted_at      TIMESTAMP    NOT NULL,
    delivered_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_logs_event_id       ON email.email_logs(event_id);
CREATE INDEX IF NOT EXISTS idx_email_logs_organizer_id   ON email.email_logs(organizer_id);
CREATE INDEX IF NOT EXISTS idx_email_logs_status         ON email.email_logs(status);

-- ── payment.subscription_plans ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment.subscription_plans (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_name             VARCHAR(50) NOT NULL UNIQUE,
    max_events            INTEGER NOT NULL DEFAULT -1,
    max_guests_per_event  INTEGER NOT NULL DEFAULT -1,
    monthly_price_kes     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    monthly_price_usd     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── payment.user_subscriptions ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment.user_subscriptions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    plan_id      UUID NOT NULL REFERENCES payment.subscription_plans(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    renewal_date DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '30 days')::DATE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON payment.user_subscriptions(user_id);

-- ── payment.payment_transactions ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment.payment_transactions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES auth.users(id),
    plan_id            UUID NOT NULL REFERENCES payment.subscription_plans(id),
    provider           VARCHAR(20) NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    currency           VARCHAR(5) NOT NULL DEFAULT 'KES',
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(200),
    metadata           TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_user_id ON payment.payment_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status  ON payment.payment_transactions(status);

-- ── payment.upgrade_requests ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment.upgrade_requests (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES auth.users(id),
    requested_plan VARCHAR(50),
    admin_notes    TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Seed: subscription plans ─────────────────────────────────────────────────
INSERT INTO payment.subscription_plans (name, max_events, max_guests_per_event, price_kes, price_usd)
VALUES
    ('FREE',       5,    500,   0,      0),
    ('PRO',        NULL, 10000, 2999,   19.99),
    ('ENTERPRISE', NULL, NULL,  9999,   79.99)
ON CONFLICT (name) DO NOTHING;

-- ── Seed: admin users (password: Admin@1234) ────────────────────────────────
INSERT INTO auth.users (username, email, password, full_name, role, status, email_verified)
VALUES 
  ('super_admin', 'superadmin@turnout.com', '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y', 'Super Admin', 'SUPER_ADMIN', 'ACTIVE', true),
  ('admin', 'admin@turnout.com', '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y', 'Turnout Admin', 'ADMIN', 'ACTIVE', true)
ON CONFLICT (email) DO NOTHING;