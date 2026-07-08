-- ================================================================
-- V2: Seed email logs from guests data
-- ================================================================

INSERT INTO email.email_logs (event_type, recipient_email, recipient_name, subject, status, provider_response, attempted_at, delivered_at)
SELECT
  'GUEST_INVITATION',
  g.email,
  g.full_name,
  'You are invited: ' || e.title,
  CASE WHEN g.token_used = true THEN 'DELIVERED' ELSE 'SENT' END,
  CASE WHEN g.token_used = true THEN '{"messageId":"brevo-ok"}' ELSE NULL END,
  g.created_at + INTERVAL '1 minute',
  CASE WHEN g.token_used = true THEN g.created_at + INTERVAL '2 minutes' ELSE NULL END
FROM guests.guests g
JOIN events.events e ON e.id = g.event_id;
