/**
 * Turnout — Admin Panel Smoke Test
 *
 * Fast, shallow, broad check that every endpoint the React Admin Panel calls
 * is alive and returns the expected status code through the API gateway.
 * This is NOT a deep business-logic test (see Phase 12 unit/integration tests
 * for that) — it's a "did the deploy break anything" check, meant to run in
 * seconds after every docker-compose up / restart.
 *
 * USAGE
 *   k6 run smoke-test-admin-panel.js
 *
 * REQUIRED ENV VARS
 *   SUPER_ADMIN_USER, SUPER_ADMIN_PASS   — must exist and have role SUPER_ADMIN
 *
 * OPTIONAL ENV VARS (checks involving these roles are skipped with a warning
 * if not provided — the script will still run, just with reduced coverage)
 *   ADMIN_USER, ADMIN_PASS               — role ADMIN
 *   ORGANIZER_USER, ORGANIZER_PASS       — role EVENT_ORGANIZER (for 403 checks)
 *   BASE_URL                             — default http://localhost:8080
 *   CREATE_TEST_ADMIN=true               — actually exercises POST /api/admin/create-admin
 *                                           happy path (creates a real, disposable ADMIN
 *                                           account). Off by default to avoid DB bloat.
 *
 * Example:
 *   k6 run -e SUPER_ADMIN_USER=superadmin -e SUPER_ADMIN_PASS=changeme \
 *          -e ADMIN_USER=admin1 -e ADMIN_PASS=changeme \
 *          -e ORGANIZER_USER=organizer1 -e ORGANIZER_PASS=changeme \
 *          smoke-test-admin-panel.js
 */

import http from 'k6/http';
import { check, group } from 'k6';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate>0.95'], // smoke test: almost everything should pass, every run
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SUPER_ADMIN_USER = __ENV.SUPER_ADMIN_USER || '';
const SUPER_ADMIN_PASS = __ENV.SUPER_ADMIN_PASS || '';
const ADMIN_USER = __ENV.ADMIN_USER || '';
const ADMIN_PASS = __ENV.ADMIN_PASS || '';
const ORGANIZER_USER = __ENV.ORGANIZER_USER || '';
const ORGANIZER_PASS = __ENV.ORGANIZER_PASS || '';
const CREATE_TEST_ADMIN = (__ENV.CREATE_TEST_ADMIN || 'false') === 'true';

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

function authHeaders(token) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
}

function login(username, password) {
  if (!username || !password) return null;
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username, password }),
    JSON_HEADERS
  );
  const ok = check(res, {
    [`login (${username}) → 200`]: (r) => r.status === 200,
    [`login (${username}) → has accessToken`]: (r) => !!r.json('accessToken'),
  });
  return ok ? res.json('accessToken') : null;
}

export default function () {
  // ---------------------------------------------------------------
  // 0. PRE-FLIGHT — fail fast if a service is down before anything else
  // ---------------------------------------------------------------
  let superAdminToken;
  group('0. Pre-flight', () => {
    superAdminToken = login(SUPER_ADMIN_USER, SUPER_ADMIN_PASS);
    if (!superAdminToken) {
      console.error('Cannot proceed without a working SUPER_ADMIN login — aborting smoke test.');
      return;
    }

    const health = http.get(`${BASE_URL}/api/admin/system-health`, authHeaders(superAdminToken));
    check(health, {
      'system-health → 200': (r) => r.status === 200,
    });
    if (health.status === 200) {
      const body = health.json();
      console.log(`System health: ${body.overallStatus} — ${JSON.stringify(body.serviceStatuses)}`);
      check(health, {
        'system-health → overallStatus is HEALTHY': () => body.overallStatus === 'HEALTHY',
      });
    }
  });

  if (!superAdminToken) return; // nothing else will work without auth

  // ---------------------------------------------------------------
  // 1. AUTH
  // ---------------------------------------------------------------
  let adminToken, organizerToken;
  group('1. Authentication', () => {
    const me = http.get(`${BASE_URL}/api/auth/me`, authHeaders(superAdminToken));
    check(me, { 'GET /api/auth/me → 200': (r) => r.status === 200 });

    const refresh = http.post(`${BASE_URL}/api/auth/refresh`, null, authHeaders(superAdminToken));
    check(refresh, { 'POST /api/auth/refresh → 200': (r) => r.status === 200 });

    if (ADMIN_USER) {
      adminToken = login(ADMIN_USER, ADMIN_PASS);
    } else {
      console.warn('ADMIN_USER not set — skipping ADMIN-role checks.');
    }

    if (ORGANIZER_USER) {
      organizerToken = login(ORGANIZER_USER, ORGANIZER_PASS);
    } else {
      console.warn('ORGANIZER_USER not set — skipping EVENT_ORGANIZER 403 checks.');
    }
  });

  // ---------------------------------------------------------------
  // 2. DASHBOARD
  // ---------------------------------------------------------------
  group('2. Dashboard', () => {
    const stats = http.get(`${BASE_URL}/api/admin/dashboard/stats`, authHeaders(superAdminToken));
    check(stats, { 'GET /api/admin/dashboard/stats → 200': (r) => r.status === 200 });

    const recent = http.get(`${BASE_URL}/api/admin/dashboard/recent-rsvps`, authHeaders(superAdminToken));
    check(recent, { 'GET /api/admin/dashboard/recent-rsvps → 200': (r) => r.status === 200 });

    if (organizerToken) {
      const forbidden = http.get(`${BASE_URL}/api/admin/dashboard/stats`, authHeaders(organizerToken));
      check(forbidden, { 'dashboard/stats as EVENT_ORGANIZER → 403': (r) => r.status === 403 });
    }
  });

  // ---------------------------------------------------------------
  // 3. ORGANIZERS PAGE
  // ---------------------------------------------------------------
  let sampleOrganizerId, originalStatus;
  group('3. Organizers', () => {
    const list = http.get(`${BASE_URL}/api/admin/users`, authHeaders(superAdminToken));
    check(list, { 'GET /api/admin/users → 200': (r) => r.status === 200 });

    const searchRes = http.get(`${BASE_URL}/api/admin/users?search=a`, authHeaders(superAdminToken));
    check(searchRes, { 'GET /api/admin/users?search=a → 200': (r) => r.status === 200 });

    if (organizerToken) {
      const forbidden = http.get(`${BASE_URL}/api/admin/users`, authHeaders(organizerToken));
      check(forbidden, { 'GET /api/admin/users as EVENT_ORGANIZER → 403': (r) => r.status === 403 });
    }

    if (list.status === 200) {
      const body = list.json();
      const content = body.content || body; // tolerate either Page<> or raw array shape
      if (Array.isArray(content) && content.length > 0) {
        sampleOrganizerId = content[0].id;
        originalStatus = content[0].status;
      } else {
        console.warn('No organizer accounts found — detail/suspend checks will be skipped.');
      }
    }

    if (sampleOrganizerId) {
      const detail = http.get(`${BASE_URL}/api/admin/users/${sampleOrganizerId}`, authHeaders(superAdminToken));
      check(detail, { 'GET /api/admin/users/{id} → 200': (r) => r.status === 200 });

      // Toggle suspend then immediately restore — idempotent round trip
      const toSuspend = originalStatus !== 'SUSPENDED';
      const toggle1 = http.patch(
        `${BASE_URL}/api/admin/users/${sampleOrganizerId}/suspend`,
        JSON.stringify({ suspend: toSuspend }),
        authHeaders(superAdminToken)
      );
      check(toggle1, { 'PATCH .../suspend (toggle) → 200': (r) => r.status === 200 });

      const toggle2 = http.patch(
        `${BASE_URL}/api/admin/users/${sampleOrganizerId}/suspend`,
        JSON.stringify({ suspend: !toSuspend }),
        authHeaders(superAdminToken)
      );
      check(toggle2, { 'PATCH .../suspend (restore) → 200': (r) => r.status === 200 });
    }
  });

  // ---------------------------------------------------------------
  // 4. CREATE-ADMIN
  // ---------------------------------------------------------------
  group('4. Admin Account Creation', () => {
    if (adminToken) {
      const asAdmin = http.post(
        `${BASE_URL}/api/admin/create-admin`,
        JSON.stringify({
          username: 'smoke_admin_check',
          email: 'smoke_admin_check@example.com',
          fullName: 'Smoke Test',
          password: 'irrelevant-should-403',
        }),
        authHeaders(adminToken)
      );
      check(asAdmin, { 'POST /api/admin/create-admin as ADMIN → 403': (r) => r.status === 403 });
    }

    if (CREATE_TEST_ADMIN) {
      const stamp = Date.now();
      const asSuperAdmin = http.post(
        `${BASE_URL}/api/admin/create-admin`,
        JSON.stringify({
          username: `smoke_admin_${stamp}`,
          email: `smoke_admin_${stamp}@example.com`,
          fullName: 'Smoke Test Admin',
          password: 'SmokeTest123!',
        }),
        authHeaders(superAdminToken)
      );
      check(asSuperAdmin, { 'POST /api/admin/create-admin as SUPER_ADMIN → 201': (r) => r.status === 201 });
    } else {
      console.log('CREATE_TEST_ADMIN not set — skipping live admin-creation happy path (set =true to test it).');
    }
  });

  // ---------------------------------------------------------------
  // 5. EVENTS BROWSER PAGE
  // ---------------------------------------------------------------
  group('5. Events Browser', () => {
    const all = http.get(`${BASE_URL}/api/events`, authHeaders(superAdminToken));
    check(all, { 'GET /api/events → 200': (r) => r.status === 200 });

    const filtered = http.get(`${BASE_URL}/api/events?status=ACTIVE`, authHeaders(superAdminToken));
    check(filtered, { 'GET /api/events?status=ACTIVE → 200': (r) => r.status === 200 });

    const dateFiltered = http.get(
      `${BASE_URL}/api/events?dateFrom=2026-01-01T00:00:00&dateTo=2026-12-31T23:59:59`,
      authHeaders(superAdminToken)
    );
    check(dateFiltered, { 'GET /api/events with date range → 200': (r) => r.status === 200 });

    const paged = http.get(`${BASE_URL}/api/events?page=0&size=5`, authHeaders(superAdminToken));
    check(paged, { 'GET /api/events?page=0&size=5 → 200': (r) => r.status === 200 });
  });

  // ---------------------------------------------------------------
  // 6. PAYMENTS PAGE & ENTERPRISE REQUESTS PAGE
  // ---------------------------------------------------------------
  group('6. Payments & Enterprise Requests', () => {
    const plans = http.get(`${BASE_URL}/api/payments/plans`, authHeaders(superAdminToken));
    check(plans, { 'GET /api/payments/plans → 200': (r) => r.status === 200 });

    const txns = http.get(`${BASE_URL}/api/payments/transactions/all`, authHeaders(superAdminToken));
    check(txns, { 'GET /api/payments/transactions/all → 200': (r) => r.status === 200 });

    const mpesaFilter = http.get(
      `${BASE_URL}/api/payments/transactions/all?provider=MPESA`,
      authHeaders(superAdminToken)
    );
    check(mpesaFilter, { 'GET .../transactions/all?provider=MPESA → 200': (r) => r.status === 200 });

    if (organizerToken) {
      const forbidden = http.get(`${BASE_URL}/api/payments/transactions/all`, authHeaders(organizerToken));
      check(forbidden, { 'transactions/all as EVENT_ORGANIZER → 403': (r) => r.status === 403 });
    }

    const requests = http.get(`${BASE_URL}/api/payments/upgrade/requests`, authHeaders(superAdminToken));
    check(requests, { 'GET /api/payments/upgrade/requests → 200': (r) => r.status === 200 });

    const approvedOnly = http.get(
      `${BASE_URL}/api/payments/upgrade/requests?status=APPROVED`,
      authHeaders(superAdminToken)
    );
    check(approvedOnly, { 'GET .../upgrade/requests?status=APPROVED → 200': (r) => r.status === 200 });

    // Approve/reject against a non-existent id — checks routing + role-gate without
    // touching a real pending request. Expect 404 once past the role check.
    const fakeId = '00000000-0000-0000-0000-000000000000';
    const approve = http.patch(`${BASE_URL}/api/payments/upgrade/approve/${fakeId}`, null, authHeaders(superAdminToken));
    check(approve, { 'PATCH .../upgrade/approve/{fakeId} → 404': (r) => r.status === 404 });

    if (organizerToken) {
      const approveForbidden = http.patch(
        `${BASE_URL}/api/payments/upgrade/approve/${fakeId}`,
        null,
        authHeaders(organizerToken)
      );
      check(approveForbidden, { 'upgrade/approve as EVENT_ORGANIZER → 403': (r) => r.status === 403 });
    }
  });

  // ---------------------------------------------------------------
  // 7. SETTINGS PAGE (SUPER_ADMIN only)
  // ---------------------------------------------------------------
  group('7. Settings', () => {
    const plans = http.get(`${BASE_URL}/api/payments/plans`, authHeaders(superAdminToken));

    if (plans.status === 200) {
      const planList = plans.json();
      const planArray = planList.content || planList;
      if (Array.isArray(planArray) && planArray.length > 0) {
        const plan = planArray[0];

        // No-op round trip: write back the exact same values
        const roundTrip = http.put(
          `${BASE_URL}/api/payments/plans/${plan.id}`,
          JSON.stringify({
            maxEvents: plan.maxEvents,
            maxGuestsPerEvent: plan.maxGuestsPerEvent,
            monthlyPriceKes: plan.monthlyPriceKes,
            monthlyPriceUsd: plan.monthlyPriceUsd,
            active: plan.active,
          }),
          authHeaders(superAdminToken)
        );
        check(roundTrip, { 'PUT /api/payments/plans/{id} (no-op) → 200': (r) => r.status === 200 });

        if (adminToken) {
          const forbidden = http.put(
            `${BASE_URL}/api/payments/plans/${plan.id}`,
            JSON.stringify({ active: plan.active }),
            authHeaders(adminToken)
          );
          check(forbidden, { 'PUT /api/payments/plans/{id} as ADMIN → 403': (r) => r.status === 403 });
        }
      } else {
        console.warn('No subscription plans found — plan-edit round trip skipped.');
      }
    }
  });

  // ---------------------------------------------------------------
  // 8. CLEANUP — confirm logout actually invalidates the token
  // ---------------------------------------------------------------
  group('8. Logout', () => {
    const logout = http.post(`${BASE_URL}/api/auth/logout`, null, authHeaders(superAdminToken));
    check(logout, { 'POST /api/auth/logout → 200': (r) => r.status === 200 });

    const afterLogout = http.get(`${BASE_URL}/api/auth/me`, authHeaders(superAdminToken));
    check(afterLogout, { 'GET /api/auth/me after logout → 401 (blacklisted)': (r) => r.status === 401 });
  });
}
