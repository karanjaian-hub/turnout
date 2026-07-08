-- ================================================================
-- V1: Create email schema and tables
-- ================================================================

CREATE SCHEMA IF NOT EXISTS email;

CREATE SEQUENCE IF NOT EXISTS email.email_logs_id_seq;

CREATE TABLE IF NOT EXISTS email.email_logs (
    id               BIGINT                      NOT NULL DEFAULT nextval('email.email_logs_id_seq'),
    event_type       VARCHAR(255)                NOT NULL,
    recipient_email  VARCHAR(255)                NOT NULL,
    recipient_name   VARCHAR(255),
    subject          VARCHAR(500)                NOT NULL,
    status           VARCHAR(50)                 NOT NULL,
    provider_response TEXT,
    attempted_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    delivered_at     TIMESTAMP WITHOUT TIME ZONE,
    event_id         UUID,
    guest_id         VARCHAR(255),

    CONSTRAINT email_logs_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_email_logs_status ON email.email_logs (status);
