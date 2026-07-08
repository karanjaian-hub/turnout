-- ================================================================
-- V1: Create guests schema and table
-- ================================================================

CREATE SCHEMA IF NOT EXISTS guests;

CREATE TABLE IF NOT EXISTS guests.guests (
    id          UUID                        NOT NULL DEFAULT gen_random_uuid(),
    event_id    UUID                        NOT NULL,
    email       VARCHAR(255)                NOT NULL,
    full_name   VARCHAR(255)                NOT NULL DEFAULT '',
    token       VARCHAR(500),
    token_used  BOOLEAN                     NOT NULL DEFAULT false,
    rsvp_status VARCHAR(50)                 NOT NULL DEFAULT 'PENDING',
    rsvp_date   TIMESTAMP WITHOUT TIME ZONE,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT guests_pkey PRIMARY KEY (id),
    CONSTRAINT guests_event_id_email_key UNIQUE (event_id, email),
    CONSTRAINT guests_token_key UNIQUE (token),
    CONSTRAINT guests_event_id_fkey FOREIGN KEY (event_id)
        REFERENCES events.events(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_guests_email       ON guests.guests (email);
CREATE INDEX IF NOT EXISTS idx_guests_event_id    ON guests.guests (event_id);
CREATE INDEX IF NOT EXISTS idx_guests_rsvp_status ON guests.guests (rsvp_status);
