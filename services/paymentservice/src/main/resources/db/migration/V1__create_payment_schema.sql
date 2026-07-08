-- ================================================================
-- V1: Create payment schema and tables
-- ================================================================

CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE IF NOT EXISTS payment.subscription_plans (
    id                   UUID           NOT NULL DEFAULT gen_random_uuid(),
    plan_name            VARCHAR(50)    NOT NULL,
    max_events           INTEGER        NOT NULL DEFAULT -1,
    max_guests_per_event INTEGER        NOT NULL DEFAULT -1,
    monthly_price_kes    NUMERIC(10,2)  NOT NULL DEFAULT 0,
    monthly_price_usd    NUMERIC(10,2)  NOT NULL DEFAULT 0,
    active               BOOLEAN        NOT NULL DEFAULT true,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT subscription_plans_pkey PRIMARY KEY (id),
    CONSTRAINT subscription_plans_plan_name_key UNIQUE (plan_name)
);

CREATE TABLE IF NOT EXISTS payment.user_subscriptions (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL,
    plan_id      UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date   DATE        NOT NULL DEFAULT CURRENT_DATE,
    renewal_date DATE        NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '30 days')::date,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT user_subscriptions_pkey PRIMARY KEY (id),
    CONSTRAINT user_subscriptions_plan_id_fkey FOREIGN KEY (plan_id)
        REFERENCES payment.subscription_plans(id),
    CONSTRAINT user_subscriptions_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON payment.user_subscriptions (user_id);

CREATE TABLE IF NOT EXISTS payment.payment_transactions (
    id                 UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id            UUID          NOT NULL,
    plan_id            UUID          NOT NULL,
    provider           VARCHAR(20)   NOT NULL,
    amount             NUMERIC(12,2) NOT NULL,
    currency           VARCHAR(5)    NOT NULL DEFAULT 'KES',
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(200),
    metadata           TEXT,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT payment_transactions_pkey PRIMARY KEY (id),
    CONSTRAINT payment_transactions_plan_id_fkey FOREIGN KEY (plan_id)
        REFERENCES payment.subscription_plans(id),
    CONSTRAINT payment_transactions_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES auth.users(id)
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_status  ON payment.payment_transactions (status);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_user_id ON payment.payment_transactions (user_id);

CREATE TABLE IF NOT EXISTS payment.upgrade_requests (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL,
    requested_plan VARCHAR(50),
    admin_notes    TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT upgrade_requests_pkey PRIMARY KEY (id),
    CONSTRAINT upgrade_requests_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES auth.users(id)
);
