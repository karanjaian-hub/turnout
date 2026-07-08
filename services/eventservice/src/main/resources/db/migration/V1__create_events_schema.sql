-- ================================================================
-- V1: Create events schema and tables
-- ================================================================

CREATE SCHEMA IF NOT EXISTS events;

CREATE TABLE IF NOT EXISTS events.events (
    id                  UUID                        NOT NULL DEFAULT gen_random_uuid(),
    created_by          UUID                        NOT NULL,
    title               VARCHAR(255)                NOT NULL,
    description         TEXT,
    location            VARCHAR(500)                NOT NULL,
    event_date          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    max_capacity        INTEGER,
    current_rsvp_count  INTEGER                     NOT NULL DEFAULT 0,
    status              VARCHAR(50)                 NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT events_pkey PRIMARY KEY (id),
    CONSTRAINT events_created_by_fkey FOREIGN KEY (created_by)
        REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_events_created_by ON events.events (created_by);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events.events (event_date);
CREATE INDEX IF NOT EXISTS idx_events_status     ON events.events (status);

CREATE TABLE IF NOT EXISTS events.audit_logs (
    id          UUID                        NOT NULL DEFAULT gen_random_uuid(),
    event_id    UUID                        NOT NULL,
    user_id     UUID                        NOT NULL,
    action      VARCHAR(100)                NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    details     TEXT,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT audit_logs_pkey PRIMARY KEY (id),
    CONSTRAINT audit_logs_event_id_fkey FOREIGN KEY (event_id)
        REFERENCES events.events(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_id ON events.audit_logs (event_id);
