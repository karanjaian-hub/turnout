package com.turnout.notificationservice.model;

import com.turnout.notificationservice.dto.RsvpUpdateMessage;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory representation of the cached dashboard counters for one event.
 * Reads from and writes to a Redis hash — no JPA, no database.
 */
public class DashboardStats {

    private long confirmed;
    private long declined;
    private long maybe;
    private long pending;
    private long waitlisted;
    private long totalInvited;

    public static DashboardStats fromMap(Map<Object, Object> map) {
        DashboardStats s = new DashboardStats();
        s.confirmed    = parseLong(map, "confirmed");
        s.declined     = parseLong(map, "declined");
        s.maybe        = parseLong(map, "maybe");
        s.pending      = parseLong(map, "pending");
        s.waitlisted   = parseLong(map, "waitlisted");
        s.totalInvited = parseLong(map, "totalInvited");
        return s;
    }

    /**
     * Increment the counter matching the RSVP status string from the event.
     * Unknown statuses are ignored rather than crashing — defensive by design.
     */
    public void increment(String rsvpStatus) {
        switch (rsvpStatus.toUpperCase()) {
            case "CONFIRMED"   -> confirmed++;
            case "DECLINED"    -> declined++;
            case "MAYBE"       -> maybe++;
            case "WAITLISTED"  -> waitlisted++;
            // PENDING is the default state on invite — not incremented on submission
            default -> { /* no-op */ }
        }
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("confirmed",    String.valueOf(confirmed));
        map.put("declined",     String.valueOf(declined));
        map.put("maybe",        String.valueOf(maybe));
        map.put("pending",      String.valueOf(pending));
        map.put("waitlisted",   String.valueOf(waitlisted));
        map.put("totalInvited", String.valueOf(totalInvited));
        return map;
    }

    public RsvpUpdateMessage toRsvpUpdateMessage(UUID eventId) {
        long responded = confirmed + declined + maybe + waitlisted;
        double confirmationRate = totalInvited == 0 ? 0.0
                : Math.round((confirmed * 100.0 / totalInvited) * 10.0) / 10.0;

        return new RsvpUpdateMessage(
                eventId, confirmed, declined, maybe, pending,
                waitlisted, totalInvited, confirmationRate, LocalDateTime.now()
        );
    }

    private static long parseLong(Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0L;
        try { return Long.parseLong(val.toString()); }
        catch (NumberFormatException e) { return 0L; }
    }
}
