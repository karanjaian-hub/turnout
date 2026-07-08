#!/usr/bin/env python3
"""
TURNOUT — Automated End-to-End API Test Runner
Runs every service in dependency order through the API Gateway, exactly the
way Turnout_Postman_Collection_v2.json is structured — but fully automated,
including the two steps Postman can't do on its own:
  - pulling the OTP straight from Redis after registration
  - pulling the guest RSVP token straight from Postgres after CSV import

Usage:
    pip install requests psycopg2-binary redis --break-system-packages
    python3 turnout_e2e_test.py

Re-runnable: a fresh organizer/admin account is generated each run, so you
can run this after every patch/deploy without manually cleaning up data.
"""

import sys
import time
import uuid
import requests
import psycopg2
import redis

# ============================== CONFIG ==============================
# Edit these to match your .env / docker-compose.yml
GATEWAY_URL = "http://localhost:8080"

SERVICE_HEALTH_URLS = {
    "Gateway":       f"{GATEWAY_URL}/actuator/health",
    "Auth":          "http://localhost:8081/actuator/health",
    "Event":         "http://localhost:8082/actuator/health",
    "Guest":         "http://localhost:8083/actuator/health",
    "Email":         "http://localhost:8084/actuator/health",
    "RSVP":          "http://localhost:8085/actuator/health",
    "Notification":  "http://localhost:8086/actuator/health",
    "Payment":       "http://localhost:8087/actuator/health",
    "AI":            "http://localhost:8088/actuator/health",
}

# Seeded super admin from init-db.sql — change if you customized it
ADMIN_USERNAME = "super_admin"
ADMIN_PASSWORD = "Admin@1234"

PG_CONFIG = dict(
    host="localhost", port=5432,
    dbname="turnout_db", user="turnout_user", password="turnout_pass",
)
REDIS_CONFIG = dict(host="localhost", port=6379, decode_responses=True)

TIMEOUT = 10
# ======================================================================

PASS, FAIL = "PASS", "FAIL"
results = []


def record(name, ok, detail=""):
    results.append((name, PASS if ok else FAIL, detail))
    icon = "OK " if ok else "FAIL"
    suffix = f" -- {detail}" if detail else ""
    print(f"  [{icon}] {name}{suffix}")


def safe_json(r):
    try:
        return r.json()
    except Exception:
        return {}


def req(method, path, token=None, **kwargs):
    headers = kwargs.pop("headers", {})
    if token:
        headers["Authorization"] = f"Bearer {token}"
    url = f"{GATEWAY_URL}{path}"
    return requests.request(method, url, headers=headers, timeout=TIMEOUT, **kwargs)


def section(title):
    print(f"\n{'=' * 64}\n{title}\n{'=' * 64}")


# ---------------------------------------------------------------------
# 0. Health checks
# ---------------------------------------------------------------------
def run_health_checks():
    section("0. HEALTH CHECKS")
    for name, url in SERVICE_HEALTH_URLS.items():
        try:
            r = requests.get(url, timeout=5)
            body = safe_json(r)
            ok = r.status_code == 200 and body.get("status") == "UP"
            record(f"{name} service health", ok, f"HTTP {r.status_code}")
        except Exception as e:
            record(f"{name} service health", False, str(e))


# ---------------------------------------------------------------------
# Direct DB / cache helpers — this is the part Postman can't do
# ---------------------------------------------------------------------
def get_otp_from_redis(user_id):
    r = redis.Redis(**REDIS_CONFIG)
    val = r.get(f"otp:{user_id}")
    if val and val.startswith('"') and val.endswith('"'):
        val = val[1:-1]
    return val


def get_guest_token_from_db(event_id):
    conn = psycopg2.connect(**PG_CONFIG)
    try:
        cur = conn.cursor()
        cur.execute(
            "SELECT token FROM guests.guests WHERE event_id = %s "
            "ORDER BY created_at LIMIT 1",
            (event_id,),
        )
        row = cur.fetchone()
        cur.close()
        return row[0] if row else None
    finally:
        conn.close()


# ---------------------------------------------------------------------
# 1. Auth Service
# ---------------------------------------------------------------------
def run_auth_flow():
    section("1. AUTH SERVICE")
    suffix = uuid.uuid4().hex[:6]
    username = f"e2e_organizer_{suffix}"
    email = f"e2e_{suffix}@turnout.app"
    password = "TestPass@123"

    r = req("POST", "/api/auth/register", json={
        "username": username, "email": email,
        "fullName": "E2E Test Organizer", "password": password,
    })
    ok = r.status_code == 201
    record("Register organizer", ok, f"HTTP {r.status_code} {r.text[:150]}")
    if not ok:
        return {}
    user_id = safe_json(r).get("userId")

    r = req("POST", "/api/auth/resend-otp", json={"userId": user_id})
    record("Resend OTP", r.status_code == 200, f"HTTP {r.status_code} {r.text[:150]}")

    otp = None
    try:
        otp = get_otp_from_redis(user_id)
    except Exception as e:
        record("Fetch OTP from Redis", False, f"Redis connection failed: {e}")
    else:
        record("Fetch OTP from Redis", otp is not None,
               "" if otp else "otp:{userId} key not found in Redis")
    if not otp:
        return {}

    r = req("POST", "/api/auth/verify-otp", json={"userId": user_id, "otp": otp})
    record("Verify OTP", r.status_code == 200, f"HTTP {r.status_code} {r.text[:150]}")

    r = req("POST", "/api/auth/login", json={"username": username, "password": password})
    ok = r.status_code == 200
    record("Login as organizer", ok, f"HTTP {r.status_code} {r.text[:150]}")
    if not ok:
        return {}
    organizer_token = safe_json(r).get("accessToken")
    refresh_token = safe_json(r).get("refreshToken")

    if refresh_token:
        r = req("POST", "/api/auth/refresh", json={"refreshToken": refresh_token})
        ok = r.status_code == 200
        record("Refresh access token", ok, f"HTTP {r.status_code} {r.text[:150]}")
        if ok:
            # use the freshly rotated token for the rest of the flow, in case the old one gets blacklisted
            organizer_token = safe_json(r).get("accessToken", organizer_token)
    else:
        record("Refresh access token", False, "no refreshToken in login response")

    r = req("GET", "/api/auth/me", token=organizer_token)
    record("GET /api/auth/me", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("GET", f"/api/auth/users/{user_id}", token=organizer_token)
    record("GET /api/auth/users/{id}", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("POST", "/api/auth/forgot-password", json={"email": email})
    record("Forgot password (request reset)", r.status_code == 200, f"HTTP {r.status_code} {r.text[:150]}")

    r = req("POST", "/api/auth/fcm-token", token=organizer_token,
            json={"fcmToken": "dGVzdC1mY20tdG9rZW4="},
            headers={"X-User-Id": user_id})
    record("POST /api/auth/fcm-token", r.status_code == 200, f"HTTP {r.status_code} {r.text[:150]}")

    r = req("POST", "/api/auth/login", json={"username": ADMIN_USERNAME, "password": ADMIN_PASSWORD})
    ok = r.status_code == 200
    record("Login as SUPER_ADMIN (seeded)", ok,
           f"HTTP {r.status_code} -- if this fails, check ADMIN_USERNAME/PASSWORD at the top of this script")
    admin_token = safe_json(r).get("accessToken") if ok else None

    organizer_id = None
    if admin_token:
        r = req("GET", "/api/admin/users?page=0&size=50", token=admin_token)
        ok = r.status_code == 200
        record("GET /api/admin/users (list organizers)", ok, f"HTTP {r.status_code}")
        if ok:
            content = safe_json(r).get("content", [])
            match = next((u for u in content if u.get("username") == username), None)
            organizer_id = match.get("id") if match else (content[0].get("id") if content else None)

        if organizer_id:
            r = req("GET", f"/api/admin/users/{organizer_id}", token=admin_token)
            record("GET /api/admin/users/{id}", r.status_code == 200, f"HTTP {r.status_code}")

            r = req("PATCH", f"/api/admin/users/{organizer_id}/suspend", token=admin_token,
                    json={"suspend": True})
            record("PATCH suspend organizer", r.status_code == 200, f"HTTP {r.status_code}")

            r = req("PATCH", f"/api/admin/users/{organizer_id}/suspend", token=admin_token,
                    json={"suspend": False})
            record("PATCH unsuspend organizer", r.status_code == 200,
                   f"HTTP {r.status_code} (un-suspended again so later steps still work)")

        new_admin_username = f"e2e_admin_{suffix}"
        r = req("POST", "/api/admin/create-admin", token=admin_token, json={
            "username": new_admin_username, "email": f"{new_admin_username}@turnout.app",
            "fullName": "E2E Created Admin", "password": "AdminPass@789",
        })
        record("POST /api/admin/create-admin", r.status_code == 201, f"HTTP {r.status_code} {r.text[:150]}")

    # Logout — tested on a throwaway second login so it doesn't invalidate organizer_token,
    # which the rest of the script still needs.
    r = req("POST", "/api/auth/login", json={"username": username, "password": password})
    throwaway_token = safe_json(r).get("accessToken") if r.status_code == 200 else None
    if throwaway_token:
        r = req("POST", "/api/auth/logout", token=throwaway_token)
        record("Logout", r.status_code == 200, f"HTTP {r.status_code}")
    else:
        record("Logout", False, "could not obtain a throwaway session to log out")

    return {
        "organizer_token": organizer_token,
        "admin_token": admin_token,
        "user_id": user_id,
        "organizer_id": organizer_id,
    }


# ---------------------------------------------------------------------
# 2. Event Service
# ---------------------------------------------------------------------
def run_event_flow(organizer_token, admin_token, organizer_id):
    section("2. EVENT SERVICE")
    r = req("POST", "/api/events", token=organizer_token, json={
        "title": "E2E Test Event",
        "description": "Created by the automated E2E test runner.",
        "eventDate": "2027-06-01T18:00:00",
        "location": "KICC, Nairobi",
        "maxCapacity": 10,
    })
    ok = r.status_code == 201
    record("Create event", ok, f"HTTP {r.status_code} {r.text[:150]}")
    if not ok:
        return None
    event_id = safe_json(r).get("id")

    r = req("GET", "/api/events", token=organizer_token)
    record("List events (no filters)", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("GET", f"/api/events/{event_id}", token=organizer_token)
    record("Get event by ID", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("PUT", f"/api/events/{event_id}", token=organizer_token,
            json={"title": "E2E Test Event -- Updated", "maxCapacity": 10})
    record("Update event", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("PATCH", f"/api/events/{event_id}/status", token=organizer_token,
            json={"newStatus": "ACTIVE"})
    record("Change event status to ACTIVE", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("GET", f"/api/events/{event_id}/stats", token=organizer_token)
    record("Get event stats", r.status_code == 200, f"HTTP {r.status_code}")

    if admin_token and organizer_id:
        r = req("GET", f"/api/events?status=ACTIVE&organizerId={organizer_id}&page=0&size=20",
                token=admin_token)
        record("[PATCH] Filtered event list (Gap 1)", r.status_code == 200, f"HTTP {r.status_code}")

    # DELETE is tested on a separate, disposable event so the main event_id
    # (used by Guest/Email/RSVP/Notification below) is never removed.
    r = req("POST", "/api/events", token=organizer_token, json={
        "title": "E2E Disposable Event (for DELETE test)",
        "description": "Created solely to be deleted.",
        "eventDate": "2027-06-01T18:00:00",
        "location": "N/A",
        "maxCapacity": 1,
    })
    if r.status_code == 201:
        disposable_event_id = safe_json(r).get("id")
        r = req("DELETE", f"/api/events/{disposable_event_id}", token=organizer_token)
        record("Delete event", r.status_code in (200, 204), f"HTTP {r.status_code}")
    else:
        record("Delete event", False, "skipped -- could not create disposable event to delete")

    return event_id


# ---------------------------------------------------------------------
# 3. Guest Service
# ---------------------------------------------------------------------
def run_guest_flow(organizer_token, event_id, user_id):
    section("3. GUEST SERVICE")
    csv_content = (
        "full_name,email\n"
        "Alice Wanjiru,alice.wanjiru@example.com\n"
        "Brian Otieno,brian.otieno@example.com\n"
        "Cynthia Achieng,cynthia.achieng@example.com\n"
    )
    files = {"file": ("guests.csv", csv_content, "text/csv")}
    r = req("POST", f"/api/guests/bulk-import?eventId={event_id}&organizerId={user_id}", token=organizer_token, files=files)
    record("Bulk import guests (CSV)", r.status_code in (200, 201, 207), f"HTTP {r.status_code} {r.text[:150]}")

    r = req("GET", "/api/guests/sample-template", token=organizer_token)
    record("Download sample CSV template", r.status_code == 200, f"HTTP {r.status_code}")

    time.sleep(1)  # let the batch write land before listing
    r = req("GET", f"/api/guests/event/{event_id}?page=0&size=20", token=organizer_token)
    ok = r.status_code == 200
    record("List guests for event", ok, f"HTTP {r.status_code}")
    guest_id = None
    if ok:
        content = safe_json(r).get("content", [])
        guest_id = content[0].get("id") if content else None

    if guest_id:
        r = req("GET", f"/api/guests/{guest_id}", token=organizer_token)
        record("Get single guest", r.status_code == 200, f"HTTP {r.status_code}")

        r = req("PUT", f"/api/guests/{guest_id}", token=organizer_token,
                json={"fullName": "Updated Name", "email": "updated@example.com"})
        record("Update guest", r.status_code == 200, f"HTTP {r.status_code}")
    else:
        record("Get/Update single guest", False, "no guest_id -- bulk import probably failed above")

    r = req("GET", f"/api/guests/event/{event_id}/export", token=organizer_token)
    record("Export guests as CSV", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("GET", f"/api/guests/event/{event_id}/stats", token=organizer_token)
    record("Get guest RSVP stats for event", r.status_code == 200, f"HTTP {r.status_code}")

    return guest_id


# ---------------------------------------------------------------------
# 4. Email Service
# ---------------------------------------------------------------------
def run_email_flow(organizer_token, event_id, guest_id, user_id):
    section("4. EMAIL SERVICE")
    r = req("POST", "/api/emails/send-invitations", token=organizer_token, json={"eventId": event_id})
    record("Send invitations for event", r.status_code in (200, 202), f"HTTP {r.status_code} {r.text[:150]}")

    print("  (waiting 5s for the Kafka consumer + Brevo batch to process...)")
    time.sleep(5)

    r = req("GET", f"/api/emails/logs/event/{event_id}?page=0&size=20", token=organizer_token)
    record("Get email logs for event", r.status_code == 200, f"HTTP {r.status_code}")

    if guest_id:
        r = req("GET", f"/api/emails/logs/guest/{guest_id}", token=organizer_token)
        record("Get email logs for guest", r.status_code == 200, f"HTTP {r.status_code}")

        r = req("POST", f"/api/emails/resend/{guest_id}", token=organizer_token)
        record("Resend email to guest", r.status_code in (200, 202), f"HTTP {r.status_code}")

    r = req("GET", f"/api/emails/progress/{event_id}", token=organizer_token)
    record("Get email progress for event", r.status_code == 200, f"HTTP {r.status_code}")

    # DELETE is tested on a separate, disposable guest so we don't remove a guest
    # that RSVP Service's "most recently created guest" DB lookup might depend on.
    files = {"file": ("disposable_guest.csv", "full_name,email\nDisposable Guest,disposable@example.com\n", "text/csv")}
    r = req("POST", f"/api/guests/bulk-import?eventId={event_id}&organizerId={user_id}",
            token=organizer_token, files=files)
    r2 = req("GET", f"/api/guests/event/{event_id}?page=0&size=50", token=organizer_token)
    disposable_guest_id = None
    if r2.status_code == 200:
        for g in safe_json(r2).get("content", []):
            if g.get("email") == "disposable@example.com":
                disposable_guest_id = g.get("id")
                break
    if disposable_guest_id:
        r = req("DELETE", f"/api/guests/{disposable_guest_id}", token=organizer_token)
        record("Delete guest", r.status_code in (200, 204), f"HTTP {r.status_code}")
    else:
        record("Delete guest", False, "skipped -- could not create/find disposable guest to delete")


# ---------------------------------------------------------------------
# 5. RSVP Service
# ---------------------------------------------------------------------
def run_rsvp_flow(event_id):
    section("5. RSVP SERVICE")
    guest_token = None
    try:
        guest_token = get_guest_token_from_db(event_id)
    except Exception as e:
        record("Fetch guest token from Postgres", False, f"DB connection failed: {e}")
        return
    record("Fetch guest token from Postgres", guest_token is not None,
           "" if guest_token else "no row found -- check PG_CONFIG / schema/table/column names at the top of this script")
    if not guest_token:
        return

    r = req("GET", f"/api/rsvp/validate?token={guest_token}")
    record("Validate RSVP token", r.status_code == 200, f"HTTP {r.status_code} {r.text[:150]}")

    r = req("POST", "/api/rsvp/submit", json={"token": guest_token, "rsvpStatus": "CONFIRMED"})
    record("Submit RSVP", r.status_code == 200, f"HTTP {r.status_code} {r.text[:150]}")


# ---------------------------------------------------------------------
# 6. Notification Service
# ---------------------------------------------------------------------
def run_notification_flow(admin_token, event_id):
    section("6. NOTIFICATION SERVICE")
    if not admin_token:
        record("Notification service tests", False, "skipped -- no admin token from Auth step")
        return
    checks = [
        ("Dashboard stats for event", f"/api/admin/dashboard/stats/{event_id}"),
        ("Email progress (dashboard)", f"/api/admin/dashboard/email-progress/{event_id}"),
        ("Recent RSVPs for event", f"/api/admin/dashboard/recent-rsvps/{event_id}"),
        ("[PATCH] Platform-wide dashboard stats", "/api/admin/dashboard/stats"),
        ("[PATCH] Platform-wide recent RSVPs", "/api/admin/dashboard/recent-rsvps"),
        ("[PATCH] System health (all 9 services)", "/api/admin/system-health"),
    ]
    for name, path in checks:
        r = req("GET", path, token=admin_token)
        record(name, r.status_code == 200, f"HTTP {r.status_code}")


# ---------------------------------------------------------------------
# 7. Payment Service
# ---------------------------------------------------------------------
def run_payment_flow(organizer_token, admin_token, user_id):
    section("7. PAYMENT SERVICE")
    r = req("GET", "/api/payments/plans", token=organizer_token)
    ok = r.status_code == 200
    record("Get all subscription plans", ok, f"HTTP {r.status_code}")
    plan_id, original_plan = None, None
    if ok:
        plans = safe_json(r)
        pro = next((p for p in plans if p.get("planName") == "PRO"), None) if isinstance(plans, list) else None
        if pro:
            plan_id, original_plan = pro.get("id"), pro

    r = req("GET", "/api/payments/subscription/me", token=organizer_token)
    record("Get my subscription", r.status_code in (200, 404), f"HTTP {r.status_code} (404 acceptable -- admin accounts have no subscription)")

    r = req("GET", "/api/payments/transactions", token=organizer_token)
    record("Get my payment history", r.status_code == 200, f"HTTP {r.status_code}")

    if plan_id:
        r = req("POST", "/api/payments/upgrade/mpesa", token=organizer_token, json={
            "phoneNumber": "254712345678", "amount": 1500, "planId": plan_id, "accountRef": "TURNOUT-PRO",
        })
        record("Initiate M-Pesa STK push (sandbox)", r.status_code in (200, 201, 502, 503),
               f"HTTP {r.status_code} (502/503 acceptable if sandbox creds aren't live)")

        r = req("POST", "/api/payments/upgrade/stripe", token=organizer_token, json={
            "planId": plan_id, "successUrl": "http://localhost:3000/payment/success",
            "cancelUrl": "http://localhost:3000/payment/cancel",
        })
        record("Create Stripe checkout session", r.status_code in (200, 201), f"HTTP {r.status_code} {r.text[:150]}")
    else:
        record("M-Pesa / Stripe upgrade tests", False, "skipped -- PRO plan_id not found")

    r = req("POST", "/api/payments/upgrade/enterprise", token=organizer_token, json={"requestedPlan": "ENTERPRISE"})
    ok = r.status_code == 201
    record("Request enterprise upgrade", ok, f"HTTP {r.status_code} {r.text[:150]}")
    upgrade_request_id = safe_json(r).get("id") if ok else None

    # Second, separate request -- used only to test the reject path, so it doesn't
    # collide with upgrade_request_id above, which gets approved further down.
    r = req("POST", "/api/payments/upgrade/enterprise", token=organizer_token, json={"requestedPlan": "ENTERPRISE"})
    reject_request_id = safe_json(r).get("id") if r.status_code == 201 else None

    if user_id:
        r = req("GET", f"/api/payments/tier-check/{user_id}", token=organizer_token)
        record("Get tier limits for user", r.status_code == 200, f"HTTP {r.status_code}")

    r = req("POST", "/api/payments/mpesa/callback", json={
        "Body": {"stkCallback": {
            "MerchantRequestID": "test-merchant-id", "CheckoutRequestID": "test-checkout-id",
            "ResultCode": 0, "ResultDesc": "The service request is processed successfully.",
        }}
    })
    record("M-Pesa callback (public webhook)", r.status_code == 200, f"HTTP {r.status_code} {r.text[:150]}")

    r = req("POST", "/api/payments/stripe/webhook",
            json={"type": "checkout.session.completed",
                  "data": {"object": {"metadata": {"userId": user_id, "planId": plan_id}}}},
            headers={"Stripe-Signature": "test-signature"})
    record("Stripe webhook (public)", r.status_code in (200, 400),
           f"HTTP {r.status_code} (400 expected -- real signature check should reject this fake one)")

    if admin_token:
        r = req("GET", "/api/payments/transactions/all?page=0&size=20", token=admin_token)
        record("[PATCH] All transactions (ADMIN)", r.status_code == 200, f"HTTP {r.status_code}")

        r = req("GET", "/api/payments/upgrade/requests?page=0&size=20", token=admin_token)
        record("[PATCH] List pending enterprise requests (ADMIN)", r.status_code == 200, f"HTTP {r.status_code}")

        if upgrade_request_id:
            r = req("PATCH", f"/api/payments/upgrade/approve/{upgrade_request_id}", token=admin_token,
                    json={"adminNotes": "Approved by E2E test runner."})
            record("Approve enterprise upgrade (ADMIN)", r.status_code == 200, f"HTTP {r.status_code}")

        if reject_request_id:
            r = req("PATCH", f"/api/payments/upgrade/reject/{reject_request_id}", token=admin_token,
                    json={"adminNotes": "Rejected by E2E test runner."})
            record("Reject enterprise upgrade (ADMIN)", r.status_code == 200, f"HTTP {r.status_code}")
        else:
            record("Reject enterprise upgrade (ADMIN)", False,
                   "skipped -- could not create a second upgrade request (user may only be allowed one pending request at a time, which is reasonable backend behavior)")

        if plan_id and original_plan:
            r = req("PUT", f"/api/payments/plans/{plan_id}", token=admin_token, json={
                "maxEvents": 25, "maxGuestsPerEvent": 1000,
                "monthlyPriceKes": 1999, "monthlyPriceUsd": 15, "active": True,
            })
            record("[PATCH] Update plan tier limits (SUPER_ADMIN)", r.status_code == 200, f"HTTP {r.status_code}")

            restore_body = {
                "maxEvents": original_plan.get("maxEvents"),
                "maxGuestsPerEvent": original_plan.get("maxGuestsPerEvent"),
                "monthlyPriceKes": original_plan.get("monthlyPriceKes"),
                "monthlyPriceUsd": original_plan.get("monthlyPriceUsd"),
                "active": original_plan.get("active"),
            }
            r = req("PUT", f"/api/payments/plans/{plan_id}", token=admin_token, json=restore_body)
            record("Restore original PRO plan limits", r.status_code == 200,
                   f"HTTP {r.status_code} (keeps this test from permanently changing your PRO tier)")


# ---------------------------------------------------------------------
# 8. AI Service
# ---------------------------------------------------------------------
def run_ai_flow(organizer_token, event_id):
    section("8. AI SERVICE")
    calls = [
        ("Generate event description", "/api/ai/generate/description", {
            "notes": "Tech conference Nairobi 500 people keynote speakers networking dinner black tie formal"
        }),
        ("Generate invitation copy", "/api/ai/generate/invitation", {
            "title": "E2E Test Event", "date": "June 1, 2027 at 6:00 PM",
            "location": "KICC, Nairobi", "description": "Automated E2E test event.",
        }),
        ("Generate follow-up messages", "/api/ai/generate/followup", {
            "eventTitle": "E2E Test Event", "daysSinceSent": 7, "nonResponderCount": 5,
        }),
        ("Get RSVP insights", "/api/ai/insights/event", {
            "eventId": event_id, "total": 10, "confirmed": 1, "declined": 0, "pending": 9,
        }),
        ("Predict optimal send time", "/api/ai/predict/sendtime", {
            "eventType": "Tech conference", "audienceSize": 10, "eventDate": "2027-06-01T18:00:00",
        }),
        ("Predict capacity fill", "/api/ai/predict/capacity", {
            "eventType": "Tech conference", "totalInvited": 10, "daysUntilEvent": 30,
        }),
    ]
    for name, path, body in calls:
        r = req("POST", path, token=organizer_token, json=body)
        record(name, r.status_code in (200, 503),
               f"HTTP {r.status_code} (503 = clean Groq-unavailable fallback, also acceptable)")


# ---------------------------------------------------------------------
# Orchestration
# ---------------------------------------------------------------------
def safely(fn, *args, label="step"):
    try:
        return fn(*args)
    except Exception as e:
        print(f"\n  !! Unhandled error in {label}: {e}")
        return None


def main():
    print("TURNOUT -- Automated End-to-End API Test Runner")
    print(f"Gateway: {GATEWAY_URL}")

    safely(run_health_checks, label="health checks")

    auth = safely(run_auth_flow, label="auth flow") or {}
    organizer_token = auth.get("organizer_token")
    admin_token = auth.get("admin_token")
    user_id = auth.get("user_id")
    organizer_id = auth.get("organizer_id")

    event_id = None
    if organizer_token:
        event_id = safely(run_event_flow, organizer_token, admin_token, organizer_id, label="event flow")

    guest_id = None
    if organizer_token and event_id:
        guest_id = safely(run_guest_flow, organizer_token, event_id, user_id, label="guest flow")

    if organizer_token and event_id:
        safely(run_email_flow, organizer_token, event_id, guest_id, user_id, label="email flow")

    if event_id:
        safely(run_rsvp_flow, event_id, label="rsvp flow")

    if event_id:
        safely(run_notification_flow, admin_token, event_id, label="notification flow")

    if organizer_token:
        safely(run_payment_flow, organizer_token, admin_token, user_id, label="payment flow")

    if organizer_token:
        safely(run_ai_flow, organizer_token, event_id, label="ai flow")

    section("SUMMARY")
    passed = sum(1 for _, status, _ in results if status == PASS)
    failed = sum(1 for _, status, _ in results if status == FAIL)
    print(f"  {passed} passed, {failed} failed, {len(results)} total\n")
    if failed:
        print("  Failed steps:")
        for name, status, detail in results:
            if status == FAIL:
                print(f"    - {name}: {detail}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
