-- ================================================================
-- V3: Add more payment transactions for realistic payment history
-- ================================================================

INSERT INTO payment.payment_transactions (id, user_id, plan_id, provider, amount, currency, status, provider_reference, created_at)
SELECT
  gen_random_uuid(),
  u.id,
  sp.id,
  CASE WHEN i % 3 = 0 THEN 'STRIPE' ELSE 'MPESA' END,
  sp.monthly_price_kes,
  CASE WHEN i % 3 = 0 THEN 'USD' ELSE 'KES' END,
  CASE WHEN i % 8 = 0 THEN 'FAILED' WHEN i % 12 = 0 THEN 'PENDING' ELSE 'SUCCESS' END,
  CASE WHEN i % 3 = 0 THEN 'pi_' || left(md5(i::text), 20)
       ELSE upper(left(md5(i::text), 10)) END,
  now() - (i * INTERVAL '3 days')
FROM generate_series(1, 20) AS s(i)
CROSS JOIN (
  SELECT id FROM auth.users
  WHERE role = 'EVENT_ORGANIZER' AND status = 'ACTIVE'
  LIMIT 1
) u
CROSS JOIN (
  SELECT id, monthly_price_kes FROM payment.subscription_plans
  WHERE plan_name = 'PRO'
) sp
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.payment_transactions (id, user_id, plan_id, provider, amount, currency, status, provider_reference, created_at)
SELECT
  gen_random_uuid(),
  u.id,
  sp.id,
  'MPESA',
  sp.monthly_price_kes,
  'KES',
  CASE WHEN i % 6 = 0 THEN 'FAILED' ELSE 'SUCCESS' END,
  upper(left(md5((i + 100)::text), 10)),
  now() - (i * INTERVAL '5 days')
FROM generate_series(1, 15) AS s(i)
CROSS JOIN (
  SELECT id FROM auth.users
  WHERE role = 'EVENT_ORGANIZER' AND status = 'ACTIVE'
  ORDER BY created_at DESC
  LIMIT 1
) u
CROSS JOIN (
  SELECT id, monthly_price_kes FROM payment.subscription_plans
  WHERE plan_name = 'ENTERPRISE'
) sp
ON CONFLICT (id) DO NOTHING;
