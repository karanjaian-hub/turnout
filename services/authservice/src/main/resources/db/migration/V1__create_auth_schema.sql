-- ================================================================
-- V1: Create auth schema and users table
-- ================================================================

CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.users (
    id             UUID                        NOT NULL DEFAULT gen_random_uuid(),
    username       VARCHAR(100)                NOT NULL,
    email          VARCHAR(255)                NOT NULL,
    password       VARCHAR(255)                NOT NULL,
    full_name      VARCHAR(255)                NOT NULL,
    role           VARCHAR(50)                 NOT NULL DEFAULT 'EVENT_ORGANIZER',
    status         VARCHAR(50)                 NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN                     NOT NULL DEFAULT false,
    last_login_at  TIMESTAMP WITHOUT TIME ZONE,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_username_key UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON auth.users (email);
CREATE INDEX IF NOT EXISTS idx_users_role  ON auth.users (role);
