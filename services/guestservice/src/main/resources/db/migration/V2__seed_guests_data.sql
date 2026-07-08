-- ================================================================
-- V2: Seed guests data
-- ================================================================

-- Original 15 guests for Nairobi Tech Summit
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Akinyi Odhiambo',   'akinyi.odhiambo@gmail.com',     'CONFIRMED', 'TOKEN_SUMMIT_1',  true,  now() - INTERVAL '3 days', now() - INTERVAL '4 days', now() - INTERVAL '3 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Brian Njoroge',      'brian.njoroge@safaricom.co.ke', 'CONFIRMED', 'TOKEN_SUMMIT_2',  true,  now() - INTERVAL '3 days', now() - INTERVAL '4 days', now() - INTERVAL '3 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Cynthia Waweru',     'cynthia.w@microsoft.com',       'CONFIRMED', 'TOKEN_SUMMIT_3',  true,  now() - INTERVAL '2 days', now() - INTERVAL '4 days', now() - INTERVAL '2 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Dennis Otieno',      'dennis.otieno@andela.com',      'DECLINED',  'TOKEN_SUMMIT_4',  true,  now() - INTERVAL '2 days', now() - INTERVAL '4 days', now() - INTERVAL '2 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Eunice Kariuki',     'eunice.k@strathmore.edu',       'PENDING',   'TOKEN_SUMMIT_5',  false, NULL,                      now() - INTERVAL '4 days', now() - INTERVAL '4 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Festus Mwangi',      'festus@equitybank.co.ke',       'PENDING',   'TOKEN_SUMMIT_6',  false, NULL,                      now() - INTERVAL '4 days', now() - INTERVAL '4 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Grace Achieng',      'grace.achieng@google.com',      'CONFIRMED', 'TOKEN_SUMMIT_7',  true,  now() - INTERVAL '1 day',  now() - INTERVAL '4 days', now() - INTERVAL '1 day'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Hassan Abdi',        'hassan.abdi@pesalink.co.ke',    'CONFIRMED', 'TOKEN_SUMMIT_8',  true,  now() - INTERVAL '1 day',  now() - INTERVAL '4 days', now() - INTERVAL '1 day'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Irene Njoki',        'irene.njoki@ibm.com',           'WAITLISTED','TOKEN_SUMMIT_9',  true,  now() - INTERVAL '12 hours',now() - INTERVAL '4 days',now() - INTERVAL '12 hours'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'James Mutua',        'james.mutua@cellulant.com',     'PENDING',   'TOKEN_SUMMIT_10', false, NULL,                      now() - INTERVAL '4 days', now() - INTERVAL '4 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Kendi Wanjiku',      'kendi.w@flutterwave.com',       'CONFIRMED', 'TOKEN_SUMMIT_11', true,  now() - INTERVAL '8 hours', now() - INTERVAL '3 days', now() - INTERVAL '8 hours'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Linet Chebet',       'linet.chebet@oracle.com',       'PENDING',   'TOKEN_SUMMIT_12', false, NULL,                      now() - INTERVAL '3 days', now() - INTERVAL '3 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Moses Kipkorir',     'moses.k@mpesa.co.ke',           'DECLINED',  'TOKEN_SUMMIT_13', true,  now() - INTERVAL '1 day',  now() - INTERVAL '3 days', now() - INTERVAL '1 day'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Naomi Mutheu',       'naomi.m@sendy.co.ke',           'CONFIRMED', 'TOKEN_SUMMIT_14', true,  now() - INTERVAL '6 hours', now() - INTERVAL '3 days', now() - INTERVAL '6 hours'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000001', 'Patrick Omondi',     'patrick.o@jumia.com',           'PENDING',   'TOKEN_SUMMIT_15', false, NULL,                      now() - INTERVAL '2 days', now() - INTERVAL '2 days'),

-- Women in Tech Bootcamp guests
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000004', 'Amina Yusuf',        'amina.yusuf@gmail.com',         'CONFIRMED', 'TOKEN_WITB_1',    true,  now() - INTERVAL '2 days', now() - INTERVAL '2 days', now() - INTERVAL '2 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000004', 'Brenda Atieno',      'brenda.atieno@yahoo.com',       'CONFIRMED', 'TOKEN_WITB_2',    true,  now() - INTERVAL '1 day',  now() - INTERVAL '2 days', now() - INTERVAL '1 day'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000004', 'Christine Wachira',  'c.wachira@moringa.school',      'PENDING',   'TOKEN_WITB_3',    false, NULL,                      now() - INTERVAL '2 days', now() - INTERVAL '2 days'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000004', 'Diana Kerubo',       'diana.kerubo@student.jkuat.ac.ke','DECLINED','TOKEN_WITB_4',    true,  now() - INTERVAL '12 hours',now() - INTERVAL '2 days',now() - INTERVAL '12 hours'),
  (gen_random_uuid(), 'aaaaaaaa-0000-0000-0000-000000000004', 'Esther Nyambura',    'esther.n@gmail.com',            'CONFIRMED', 'TOKEN_WITB_5',    true,  now() - INTERVAL '6 hours', now() - INTERVAL '2 days', now() - INTERVAL '6 hours')

ON CONFLICT (event_id, email) DO NOTHING;

-- Bulk guests for expanded events using generate_series
INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT
  gen_random_uuid(),
  'bbbbbbbb-0000-0000-0000-000000000001',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Awuor','Baraka',
         'Beatrice','Brenda','Brian','Caroline','Christine','Collins','Daniel','David',
         'Dennis','Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus',
         'Florence','Francis','George','Grace','Hassan','Hillary','Irene','Isaac',
         'James','Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki',
         'Kennedy','Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy',
         'Margaret','Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi',
         'Nicholas','Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick',
         'Paul','Peter','Philip','Purity','Rachel','Robert','Rose','Ruth',
         'Samuel','Sarah','Simon','Stephen','Susan','Tabitha','Timothy','Victor',
         'Wachira','Wanjiku','Waweru','Yvonne'])[((i-1) % 84) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.aiconf@turnout-test.co.ke',
  CASE WHEN i % 10 = 0 THEN 'DECLINED' WHEN i % 7 = 0 THEN 'WAITLISTED' WHEN i % 3 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_AICONF_' || i,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN true ELSE false END,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN now() - (random() * INTERVAL '5 days') ELSE NULL END,
  now() - INTERVAL '6 days', now() - INTERVAL '6 days'
FROM generate_series(1, 120) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT gen_random_uuid(), 'bbbbbbbb-0000-0000-0000-000000000002',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Awuor','Baraka',
         'Beatrice','Brenda','Brian','Caroline','Christine','Collins','Daniel','David',
         'Dennis','Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus',
         'Florence','Francis','George','Grace','Hassan','Hillary','Irene','Isaac',
         'James','Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki',
         'Kennedy','Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy',
         'Margaret','Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi',
         'Nicholas','Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick',
         'Paul','Peter','Philip','Purity','Rachel','Robert','Rose','Ruth',
         'Samuel','Sarah','Simon','Stephen','Susan','Tabitha','Timothy','Victor',
         'Wachira','Wanjiku','Waweru','Yvonne'])[((i-1) % 84) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.bootcamp@turnout-test.co.ke',
  CASE WHEN i % 10 = 0 THEN 'DECLINED' WHEN i % 8 = 0 THEN 'WAITLISTED' WHEN i % 3 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_BOOTCAMP_' || i,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN true ELSE false END,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN now() - (random() * INTERVAL '4 days') ELSE NULL END,
  now() - INTERVAL '4 days', now() - INTERVAL '4 days'
FROM generate_series(1, 110) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT gen_random_uuid(), 'bbbbbbbb-0000-0000-0000-000000000004',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Awuor','Baraka',
         'Beatrice','Brenda','Brian','Caroline','Christine','Collins','Daniel','David',
         'Dennis','Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus',
         'Florence','Francis','George','Grace','Hassan','Hillary','Irene','Isaac',
         'James','Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki',
         'Kennedy','Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy',
         'Margaret','Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi',
         'Nicholas','Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick',
         'Paul','Peter','Philip','Purity','Rachel','Robert','Rose','Ruth',
         'Samuel','Sarah','Simon','Stephen','Susan','Tabitha','Timothy','Victor',
         'Wachira','Wanjiku','Waweru','Yvonne'])[((i-1) % 84) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.cfo@turnout-test.co.ke',
  CASE WHEN i % 10 = 0 THEN 'DECLINED' WHEN i % 7 = 0 THEN 'WAITLISTED' WHEN i % 4 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_CFO_' || i,
  CASE WHEN i % 4 != 0 AND i % 10 != 0 THEN true ELSE false END,
  CASE WHEN i % 4 != 0 AND i % 10 != 0 THEN now() - (random() * INTERVAL '7 days') ELSE NULL END,
  now() - INTERVAL '7 days', now() - INTERVAL '7 days'
FROM generate_series(1, 100) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT gen_random_uuid(), 'bbbbbbbb-0000-0000-0000-000000000007',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Awuor','Baraka',
         'Beatrice','Brenda','Brian','Caroline','Christine','Collins','Daniel','David',
         'Dennis','Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus',
         'Florence','Francis','George','Grace','Hassan','Hillary','Irene','Isaac',
         'James','Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki',
         'Kennedy','Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy',
         'Margaret','Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi',
         'Nicholas','Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick',
         'Paul','Peter','Philip','Purity','Rachel','Robert','Rose','Ruth',
         'Samuel','Sarah','Simon','Stephen','Susan','Tabitha','Timothy','Victor',
         'Wachira','Wanjiku','Waweru','Yvonne'])[((i-1) % 84) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.jazz@turnout-test.co.ke',
  CASE WHEN i % 10 = 0 THEN 'DECLINED' WHEN i % 5 = 0 THEN 'WAITLISTED' WHEN i % 3 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_JAZZ_' || i,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN true ELSE false END,
  CASE WHEN i % 3 != 0 AND i % 10 != 0 THEN now() - (random() * INTERVAL '10 days') ELSE NULL END,
  now() - INTERVAL '10 days', now() - INTERVAL '10 days'
FROM generate_series(1, 115) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

INSERT INTO guests.guests (id, event_id, full_name, email, rsvp_status, token, token_used, rsvp_date, created_at, updated_at)
SELECT gen_random_uuid(), 'bbbbbbbb-0000-0000-0000-000000000008',
  (ARRAY['Abdi','Achieng','Adhiambo','Akinyi','Amina','Atieno','Awuor','Baraka',
         'Beatrice','Brenda','Brian','Caroline','Christine','Collins','Daniel','David',
         'Dennis','Diana','Edwin','Elizabeth','Emmanuel','Esther','Faith','Festus',
         'Florence','Francis','George','Grace','Hassan','Hillary','Irene','Isaac',
         'James','Jane','Janet','John','Joseph','Joyce','Kamau','Kariuki',
         'Kennedy','Kevin','Kigen','Kipchoge','Koech','Leah','Linet','Lucy',
         'Margaret','Mary','Mercy','Michael','Moses','Mutua','Nancy','Naomi',
         'Nicholas','Njeri','Njoki','Njoroge','Odhiambo','Omondi','Otieno','Patrick',
         'Paul','Peter','Philip','Purity','Rachel','Robert','Rose','Ruth',
         'Samuel','Sarah','Simon','Stephen','Susan','Tabitha','Timothy','Victor',
         'Wachira','Wanjiku','Waweru','Yvonne'])[((i-1) % 84) + 1] || ' ' ||
  (ARRAY['Mwangi','Otieno','Kamau','Wanjiku','Odhiambo','Kipchoge','Mutua','Kariuki',
         'Njoroge','Achieng','Waweru','Koech','Omondi','Githinji','Kimani','Mbugua',
         'Njenga','Macharia','Mugo','Ndegwa','Kinyua','Muthoni','Wekesa','Barasa',
         'Simiyu','Chesire','Rotich','Lagat','Cherono','Kiptoo','Yusuf','Abdi'])[((i-1) % 32) + 1],
  'guest' || i || '.blankets@turnout-test.co.ke',
  CASE WHEN i % 10 = 0 THEN 'DECLINED' WHEN i % 6 = 0 THEN 'WAITLISTED' WHEN i % 4 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
  'TOKEN_BLANKETS_' || i,
  CASE WHEN i % 4 != 0 AND i % 10 != 0 THEN true ELSE false END,
  CASE WHEN i % 4 != 0 AND i % 10 != 0 THEN now() - (random() * INTERVAL '8 days') ELSE NULL END,
  now() - INTERVAL '8 days', now() - INTERVAL '8 days'
FROM generate_series(1, 125) AS s(i)
ON CONFLICT (event_id, email) DO NOTHING;

-- Sync rsvp counts
UPDATE events.events SET current_rsvp_count = (
  SELECT COUNT(*) FROM guests.guests
  WHERE guests.event_id = events.id AND guests.rsvp_status = 'CONFIRMED'
) WHERE id IN (
  'aaaaaaaa-0000-0000-0000-000000000001','aaaaaaaa-0000-0000-0000-000000000004',
  'bbbbbbbb-0000-0000-0000-000000000001','bbbbbbbb-0000-0000-0000-000000000002',
  'bbbbbbbb-0000-0000-0000-000000000004','bbbbbbbb-0000-0000-0000-000000000007',
  'bbbbbbbb-0000-0000-0000-000000000008'
);
