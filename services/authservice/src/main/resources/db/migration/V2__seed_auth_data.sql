-- ================================================================
-- V2: Seed auth users
-- Password for all users: Admin@1234
-- ================================================================

INSERT INTO auth.users (id, username, email, password, full_name, role, status, email_verified, created_at, updated_at)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'superadmin', 'superadmin@turnout.app',
   '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y',
   'Super Admin', 'SUPER_ADMIN', 'ACTIVE', true, now(), now()),

  ('00000000-0000-0000-0000-000000000002', 'adminuser', 'admin@turnout.app',
   '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y',
   'Admin User', 'ADMIN', 'ACTIVE', true, now(), now()),

  ('00000000-0000-0000-0000-000000000010', 'wanjiku_events', 'wanjiku@nairobievents.co.ke',
   '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y',
   'Wanjiku Muthoni', 'EVENT_ORGANIZER', 'ACTIVE', true, now(), now()),

  ('00000000-0000-0000-0000-000000000011', 'kamau_tech', 'kamau@techkenya.io',
   '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y',
   'Kamau Githinji', 'EVENT_ORGANIZER', 'ACTIVE', true, now(), now()),

  ('00000000-0000-0000-0000-000000000012', 'omondi_corp', 'omondi@eventscorp.ke',
   '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y',
   'Omondi Otieno', 'EVENT_ORGANIZER', 'SUSPENDED', true, now(), now()),

  ('00000000-0000-0000-0000-000000000013', 'njeri_corporate', 'njeri@corporateevents.co.ke',
   '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y',
   'Njeri Kamande', 'EVENT_ORGANIZER', 'ACTIVE', true, now(), now()),

  ('00000000-0000-0000-0000-000000000014', 'kipchoge_entertainment', 'kipchoge@nairobishows.co.ke',
   '$2b$12$d/5JWusrfXZ9CRsUj24VpeYL4ongvtcAo1tLe/XvQbGd0d5ejYB.y',
   'Kipchoge Rotich', 'EVENT_ORGANIZER', 'ACTIVE', true, now(), now())

ON CONFLICT (id) DO NOTHING;
