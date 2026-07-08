-- ================================================================
-- V3: Add guests to events that currently have none
-- Completed events get realistic historical numbers
-- Active events get a good mix of statuses
-- ================================================================

-- Kenya Fintech Forum 2026 (DRAFT — 60 guests, mostly PENDING since not yet active)
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'aaaaaaaa-0000-0000-0000-000000000003',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Baraka','Beatrice',
         'Brenda','Brian','Caroline','Christine','Collins','Daniel','David','Dennis',
         'Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus','Florence',
         'Francis','George','Grace','Hassan','Hillary','Irene','Isaac','James',
         'Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki','Kennedy',
         'Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy','Margaret',
         'Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi','Nicholas',
         'Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick','Paul'])[((i-1) % 64) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.fintech@turnout-test.co.ke',
  CASE WHEN i % 10 = 0 THEN 'DECLINED' WHEN i % 5 = 0 THEN 'PENDING' ELSE 'PENDING' END,
  'TOKEN_FINTECH_' || i, false, NULL,
  now() - INTERVAL '2 days', now() - INTERVAL '2 days'
FROM generate_series(1, 60) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

-- Blockchain East Africa Summit (CANCELLED — 80 guests, all PENDING since event was cancelled)
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'aaaaaaaa-0000-0000-0000-000000000005',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Baraka','Beatrice',
         'Brenda','Brian','Caroline','Christine','Collins','Daniel','David','Dennis',
         'Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus','Florence',
         'Francis','George','Grace','Hassan','Hillary','Irene','Isaac','James',
         'Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki','Kennedy',
         'Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy','Margaret',
         'Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi','Nicholas',
         'Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick','Paul'])[((i-1) % 64) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.blockchain@turnout-test.co.ke',
  'PENDING', 'TOKEN_BLOCKCHAIN_' || i, false, NULL,
  now() - INTERVAL '9 days', now() - INTERVAL '9 days'
FROM generate_series(1, 80) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

-- Koroga Festival (COMPLETED — 1843 confirmed, seed 200 representative guests)
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'bbbbbbbb-0000-0000-0000-000000000009',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Baraka','Beatrice',
         'Brenda','Brian','Caroline','Christine','Collins','Daniel','David','Dennis',
         'Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus','Florence',
         'Francis','George','Grace','Hassan','Hillary','Irene','Isaac','James',
         'Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki','Kennedy',
         'Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy','Margaret',
         'Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi','Nicholas',
         'Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick','Paul'])[((i-1) % 64) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.koroga@turnout-test.co.ke',
  CASE WHEN i % 8 = 0 THEN 'DECLINED' WHEN i % 15 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_KOROGA_' || i, true,
  now() - INTERVAL '12 days',
  now() - INTERVAL '20 days', now() - INTERVAL '20 days'
FROM generate_series(1, 200) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

-- Nairobi Startup Mixer Q1 2026 (COMPLETED — 87 confirmed, seed 100 guests)
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'aaaaaaaa-0000-0000-0000-000000000002',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Baraka','Beatrice',
         'Brenda','Brian','Caroline','Christine','Collins','Daniel','David','Dennis',
         'Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus','Florence',
         'Francis','George','Grace','Hassan','Hillary','Irene','Isaac','James',
         'Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki','Kennedy',
         'Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy','Margaret',
         'Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi','Nicholas',
         'Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick','Paul'])[((i-1) % 64) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.mixer@turnout-test.co.ke',
  CASE WHEN i % 7 = 0 THEN 'DECLINED' WHEN i % 10 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_MIXER_' || i, true,
  now() - INTERVAL '32 days',
  now() - INTERVAL '40 days', now() - INTERVAL '40 days'
FROM generate_series(1, 100) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

-- HR Leadership Forum (ACTIVE — 90 guests)
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'bbbbbbbb-0000-0000-0000-000000000005',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Baraka','Beatrice',
         'Brenda','Brian','Caroline','Christine','Collins','Daniel','David','Dennis',
         'Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus','Florence',
         'Francis','George','Grace','Hassan','Hillary','Irene','Isaac','James',
         'Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki','Kennedy',
         'Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy','Margaret',
         'Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi','Nicholas',
         'Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick','Paul'])[((i-1) % 64) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.hrforum@turnout-test.co.ke',
  CASE WHEN i % 10 = 0 THEN 'DECLINED' WHEN i % 6 = 0 THEN 'WAITLISTED' WHEN i % 3 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_HRFORUM_' || i,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN true ELSE false END,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN now() - (random() * INTERVAL '5 days') ELSE NULL END,
  now() - INTERVAL '5 days', now() - INTERVAL '5 days'
FROM generate_series(1, 90) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

-- NSE Investor Day (COMPLETED — 278 confirmed, seed 300 guests)
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'bbbbbbbb-0000-0000-0000-000000000006',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Baraka','Beatrice',
         'Brenda','Brian','Caroline','Christine','Collins','Daniel','David','Dennis',
         'Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus','Florence',
         'Francis','George','Grace','Hassan','Hillary','Irene','Isaac','James',
         'Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki','Kennedy',
         'Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy','Margaret',
         'Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi','Nicholas',
         'Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick','Paul'])[((i-1) % 64) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.nse@turnout-test.co.ke',
  CASE WHEN i % 8 = 0 THEN 'DECLINED' WHEN i % 12 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_NSE_' || i, true,
  now() - INTERVAL '6 days',
  now() - INTERVAL '15 days', now() - INTERVAL '15 days'
FROM generate_series(1, 300) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

-- Sync rsvp counts for all affected events
UPDATE events.events SET current_rsvp_count = (
  SELECT COUNT(*) FROM guests.guests
  WHERE guests.event_id = events.id AND guests.rsvp_status = 'CONFIRMED'
) WHERE id IN (
  'aaaaaaaa-0000-0000-0000-000000000002',
  'aaaaaaaa-0000-0000-0000-000000000003',
  'aaaaaaaa-0000-0000-0000-000000000005',
  'bbbbbbbb-0000-0000-0000-000000000005',
  'bbbbbbbb-0000-0000-0000-000000000006',
  'bbbbbbbb-0000-0000-0000-000000000009'
);
