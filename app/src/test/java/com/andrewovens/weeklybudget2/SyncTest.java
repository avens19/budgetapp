package com.andrewovens.weeklybudget2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Test;

public class SyncTest {

    /**
     * The categories and expenses feeds are two separate requests, so their
     * watermarks are two instants. Storing the later one would skip anything
     * written to the other collection in between.
     */
    @Test
    public void nextWatermark_takesTheEarlierOfTheTwo() {
        String earlier = "2026-08-15T20:14:33.1234567Z";
        String later = "2026-08-15T20:14:35.0000000Z";

        assertEquals(earlier, Sync.nextWatermark(earlier, later));
        assertEquals(earlier, Sync.nextWatermark(later, earlier));
    }

    @Test
    public void nextWatermark_handlesEqualStamps() {
        String same = "2026-08-15T20:14:33.1234567Z";

        assertEquals(same, Sync.nextWatermark(same, same));
    }

    /**
     * Ordering the fixed-width stamps as text has to agree with ordering them
     * chronologically, including across a second and a day boundary where the
     * digits carry.
     */
    @Test
    public void nextWatermark_ordersAcrossCarries() {
        assertEquals("2026-08-15T20:14:33.9999999Z",
                Sync.nextWatermark("2026-08-15T20:14:33.9999999Z", "2026-08-15T20:14:34.0000000Z"));
        assertEquals("2026-08-15T23:59:59.9999999Z",
                Sync.nextWatermark("2026-08-16T00:00:00.0000000Z", "2026-08-15T23:59:59.9999999Z"));
        assertEquals("2026-08-09T00:00:00.0000000Z",
                Sync.nextWatermark("2026-08-09T00:00:00.0000000Z", "2026-08-10T00:00:00.0000000Z"));
    }

    /**
     * A server that sends no header gives no answer, and the caller must not
     * invent one — inventing one from the device clock is the bug this
     * replaced.
     */
    @Test
    public void nextWatermark_isNullWhenEitherHeaderIsMissing() {
        String stamp = "2026-08-15T20:14:33.1234567Z";

        assertNull(Sync.nextWatermark(null, stamp));
        assertNull(Sync.nextWatermark(stamp, null));
        assertNull(Sync.nextWatermark(null, null));
    }

    /**
     * The server's budget payload has no Watermark field. Parsing it used to
     * yield "" rather than null, which {@link Budget#update} then wrote over
     * the real stored watermark.
     */
    @Test
    public void budgetFromServerDoesNotClobberTheStoredWatermark() throws Exception {
        Budget stored = new Budget(false);
        stored.UniqueId = "3f2a9c14-6b7d-4e21-9a05-8c1f2d7e4b60";
        stored.Watermark = "2026-08-15T20:14:33.1234567Z";
        stored.Amount = 450;
        stored.StartDay = 1;

        JSONObject fromServer = new JSONObject()
                .put("UniqueId", stored.UniqueId)
                .put("Name", "Household")
                .put("StartDay", 1)
                .put("Amount", 450);

        Budget updated = Budget.update(stored, Budget.fromJson(fromServer));

        assertEquals("2026-08-15T20:14:33.1234567Z", updated.Watermark);
        assertEquals("Household", updated.Name);
    }
}
