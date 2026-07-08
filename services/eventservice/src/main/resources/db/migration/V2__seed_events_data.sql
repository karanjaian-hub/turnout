-- ================================================================
-- V2: Seed events data
-- ================================================================

INSERT INTO events.events (id, title, description, event_date, location, max_capacity, current_rsvp_count, created_by, status, created_at, updated_at)
VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001',
   'Nairobi Tech Summit 2026',
   'Annual flagship tech conference bringing together 300 of Kenya''s top engineers, product managers and founders. Keynotes, workshops, and networking dinner at the KICC.',
   now() + INTERVAL '14 days', 'Kenyatta International Convention Centre, Nairobi',
   300, 7, '00000000-0000-0000-0000-000000000010', 'ACTIVE', now() - INTERVAL '5 days', now() - INTERVAL '5 days'),

  ('aaaaaaaa-0000-0000-0000-000000000002',
   'Nairobi Startup Mixer — Q1 2026',
   'Quarterly startup networking mixer for early-stage founders and investors. Held at iHub.',
   now() - INTERVAL '30 days', 'iHub, Ngong Road, Nairobi',
   100, 87, '00000000-0000-0000-0000-000000000010', 'COMPLETED', now() - INTERVAL '45 days', now() - INTERVAL '30 days'),

  ('aaaaaaaa-0000-0000-0000-000000000003',
   'Kenya Fintech Forum 2026',
   'A half-day forum exploring the future of mobile money, embedded finance, and DeFi in East Africa.',
   now() + INTERVAL '45 days', 'Radisson Blu Hotel, Upper Hill, Nairobi',
   200, 0, '00000000-0000-0000-0000-000000000010', 'DRAFT', now() - INTERVAL '2 days', now() - INTERVAL '2 days'),

  ('aaaaaaaa-0000-0000-0000-000000000004',
   'Women in Tech Kenya Bootcamp',
   'Three-day intensive bootcamp for women entering the tech industry.',
   now() + INTERVAL '21 days', 'Moringa School, Karen, Nairobi',
   60, 3, '00000000-0000-0000-0000-000000000011', 'ACTIVE', now() - INTERVAL '3 days', now() - INTERVAL '3 days'),

  ('aaaaaaaa-0000-0000-0000-000000000005',
   'Blockchain East Africa Summit',
   'Summit cancelled due to venue unavailability.',
   now() + INTERVAL '7 days', 'Villa Rosa Kempinski, Nairobi',
   500, 0, '00000000-0000-0000-0000-000000000011', 'CANCELLED', now() - INTERVAL '10 days', now() - INTERVAL '1 day'),

  ('bbbbbbbb-0000-0000-0000-000000000001',
   'Nairobi AI & Machine Learning Conference',
   'A deep-dive conference covering practical applications of AI in African markets.',
   now() + INTERVAL '20 days', 'Strathmore University, Madaraka Estate, Nairobi',
   400, 0, '00000000-0000-0000-0000-000000000010', 'ACTIVE', now() - INTERVAL '6 days', now() - INTERVAL '6 days'),

  ('bbbbbbbb-0000-0000-0000-000000000002',
   'East Africa Developer Bootcamp 2026',
   'Five-day intensive bootcamp covering cloud-native development, Kubernetes, and microservices.',
   now() + INTERVAL '35 days', 'Moringa School, Karen, Nairobi',
   80, 0, '00000000-0000-0000-0000-000000000011', 'ACTIVE', now() - INTERVAL '4 days', now() - INTERVAL '4 days'),

  ('bbbbbbbb-0000-0000-0000-000000000003',
   'Nairobi Startup Demo Day — Q2 2026',
   'Twenty early-stage startups pitch to a panel of investors.',
   now() + INTERVAL '10 days', 'iHub, Ngong Road, Nairobi',
   250, 0, '00000000-0000-0000-0000-000000000010', 'ACTIVE', now() - INTERVAL '3 days', now() - INTERVAL '3 days'),

  ('bbbbbbbb-0000-0000-0000-000000000004',
   'Kenya CFO Summit 2026',
   'Annual gathering of 200 chief financial officers from Kenya''s top corporations.',
   now() + INTERVAL '18 days', 'Radisson Blu Hotel, Upper Hill, Nairobi',
   200, 0, '00000000-0000-0000-0000-000000000013', 'ACTIVE', now() - INTERVAL '7 days', now() - INTERVAL '7 days'),

  ('bbbbbbbb-0000-0000-0000-000000000005',
   'HR Leadership Forum — Future of Work Kenya',
   'Half-day forum exploring remote work policies, talent retention, and DEI in Kenyan corporates.',
   now() + INTERVAL '25 days', 'Villa Rosa Kempinski, Nairobi',
   150, 0, '00000000-0000-0000-0000-000000000013', 'ACTIVE', now() - INTERVAL '5 days', now() - INTERVAL '5 days'),

  ('bbbbbbbb-0000-0000-0000-000000000006',
   'NSE Listed Companies Investor Day',
   'Full-day investor relations event for NSE-listed companies.',
   now() - INTERVAL '5 days', 'Nairobi Securities Exchange, Westlands',
   300, 278, '00000000-0000-0000-0000-000000000013', 'COMPLETED', now() - INTERVAL '20 days', now() - INTERVAL '5 days'),

  ('bbbbbbbb-0000-0000-0000-000000000007',
   'Nairobi Jazz Festival 2026',
   'Three-day outdoor jazz festival at Uhuru Gardens featuring 30 artists.',
   now() + INTERVAL '30 days', 'Uhuru Gardens, Langata Road, Nairobi',
   5000, 0, '00000000-0000-0000-0000-000000000014', 'ACTIVE', now() - INTERVAL '10 days', now() - INTERVAL '10 days'),

  ('bbbbbbbb-0000-0000-0000-000000000008',
   'Blankets & Wine — June Edition',
   'Monthly outdoor music experience at Ngong Racecourse.',
   now() + INTERVAL '8 days', 'Ngong Racecourse, Ngong Road, Nairobi',
   3000, 0, '00000000-0000-0000-0000-000000000014', 'ACTIVE', now() - INTERVAL '8 days', now() - INTERVAL '8 days'),

  ('bbbbbbbb-0000-0000-0000-000000000009',
   'Koroga Festival — East African Vibes',
   'Two-day cultural music festival celebrating East African sounds.',
   now() - INTERVAL '10 days', 'Two Rivers Mall Amphitheatre, Runda, Nairobi',
   2000, 1843, '00000000-0000-0000-0000-000000000014', 'COMPLETED', now() - INTERVAL '25 days', now() - INTERVAL '10 days')

ON CONFLICT (id) DO NOTHING;

-- Audit logs
INSERT INTO events.audit_logs (id, event_id, user_id, action, entity_type, entity_id, details, created_at)
VALUES
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000010',
   'CREATE', 'Event', 'aaaaaaaa-0000-0000-0000-000000000001', '{"title":"Nairobi Tech Summit 2026"}', now() - INTERVAL '5 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000010',
   'STATUS_CHANGE', 'Event', 'aaaaaaaa-0000-0000-0000-000000000002', '{"from":"ACTIVE","to":"COMPLETED"}', now() - INTERVAL '30 days'),
  (gen_random_uuid(), 'bbbbbbbb-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000011',
   'STATUS_CHANGE', 'Event', 'aaaaaaaa-0000-0000-0000-000000000005', '{"from":"ACTIVE","to":"CANCELLED"}', now() - INTERVAL '1 day');
