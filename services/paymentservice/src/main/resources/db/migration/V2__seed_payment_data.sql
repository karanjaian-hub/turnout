-- ================================================================
-- V2: Seed payment data
-- ================================================================

-- Subscription plans
INSERT INTO payment.subscription_plans (id, plan_name, max_events, max_guests_per_event, monthly_price_kes, monthly_price_usd, active)
VALUES
  (gen_random_uuid(), 'FREE',       5,   500,    0,     0.00,  true),
  (gen_random_uuid(), 'PRO',       -1, 10000, 2999,   22.99,  true),
  (gen_random_uuid(), 'ENTERPRISE',-1,    -1, 9999,   79.99,  true)
ON CONFLICT (plan_name) DO NOTHING;

-- User subscriptions (lookup real plan IDs)
INSERT INTO payment.user_subscriptions (id, user_id, plan_id, status, start_date, renewal_date, created_at, updated_at)
SELECT '22222222-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000010',
  sp.id, 'ACTIVE', (now() - INTERVAL '15 days')::date, (now() + INTERVAL '15 days')::date,
  now() - INTERVAL '15 days', now() - INTERVAL '15 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'PRO'
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.user_subscriptions (id, user_id, plan_id, status, start_date, renewal_date, created_at, updated_at)
SELECT '22222222-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000011',
  sp.id, 'ACTIVE', (now() - INTERVAL '10 days')::date, (now() + INTERVAL '20 days')::date,
  now() - INTERVAL '10 days', now() - INTERVAL '10 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'FREE'
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.user_subscriptions (id, user_id, plan_id, status, start_date, renewal_date, created_at, updated_at)
SELECT '22222222-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000012',
  sp.id, 'SUSPENDED', (now() - INTERVAL '8 days')::date, (now() + INTERVAL '22 days')::date,
  now() - INTERVAL '8 days', now() - INTERVAL '2 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'ENTERPRISE'
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.user_subscriptions (id, user_id, plan_id, status, start_date, renewal_date, created_at, updated_at)
SELECT '22222222-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000013',
  sp.id, 'ACTIVE', (now() - INTERVAL '12 days')::date, (now() + INTERVAL '18 days')::date,
  now() - INTERVAL '12 days', now() - INTERVAL '12 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'PRO'
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.user_subscriptions (id, user_id, plan_id, status, start_date, renewal_date, created_at, updated_at)
SELECT '22222222-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000014',
  sp.id, 'ACTIVE', (now() - INTERVAL '8 days')::date, (now() + INTERVAL '22 days')::date,
  now() - INTERVAL '8 days', now() - INTERVAL '8 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'ENTERPRISE'
ON CONFLICT (id) DO NOTHING;

-- Payment transactions
INSERT INTO payment.payment_transactions (id, user_id, plan_id, provider, amount, currency, status, provider_reference, created_at)
SELECT '33333333-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000010',
  sp.id, 'MPESA', 2999.00, 'KES', 'SUCCESS', 'QHG2XKJL4Y', now() - INTERVAL '15 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'PRO'
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.payment_transactions (id, user_id, plan_id, provider, amount, currency, status, provider_reference, created_at)
SELECT '33333333-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000011',
  sp.id, 'STRIPE', 0.00, 'USD', 'SUCCESS', 'pi_3OKqKcL8example001', now() - INTERVAL '10 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'FREE'
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.payment_transactions (id, user_id, plan_id, provider, amount, currency, status, provider_reference, created_at)
SELECT '33333333-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000012',
  sp.id, 'MPESA', 9999.00, 'KES', 'SUCCESS', 'RKL9PFMZ2T', now() - INTERVAL '8 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'ENTERPRISE'
ON CONFLICT (id) DO NOTHING;

INSERT INTO payment.payment_transactions (id, user_id, plan_id, provider, amount, currency, status, provider_reference, created_at)
SELECT '33333333-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000010',
  sp.id, 'MPESA', 2999.00, 'KES', 'FAILED', 'TIMEOUT_ERR', now() - INTERVAL '16 days'
FROM payment.subscription_plans sp WHERE sp.plan_name = 'PRO'
ON CONFLICT (id) DO NOTHING;

-- Upgrade requests
INSERT INTO payment.upgrade_requests (id, user_id, requested_plan, status, admin_notes, created_at, updated_at)
VALUES
  ('44444444-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000011', 'ENTERPRISE', 'PENDING',  NULL,                            now() - INTERVAL '2 days',  now() - INTERVAL '2 days'),
  (gen_random_uuid(),                      '00000000-0000-0000-0000-000000000010', 'ENTERPRISE', 'APPROVED', 'Verified large event organizer', now() - INTERVAL '5 days',  now() - INTERVAL '4 days'),
  (gen_random_uuid(),                      '00000000-0000-0000-0000-000000000011', 'PRO',        'REJECTED', 'Insufficient event history',     now() - INTERVAL '3 days',  now() - INTERVAL '2 days'),
  (gen_random_uuid(),                      '00000000-0000-0000-0000-000000000013', 'ENTERPRISE', 'PENDING',  NULL,                            now() - INTERVAL '1 day',   now() - INTERVAL '1 day'),
  (gen_random_uuid(),                      '00000000-0000-0000-0000-000000000014', 'PRO',        'PENDING',  NULL,                            now() - INTERVAL '12 hours', now() - INTERVAL '12 hours')
ON CONFLICT (id) DO NOTHING;
