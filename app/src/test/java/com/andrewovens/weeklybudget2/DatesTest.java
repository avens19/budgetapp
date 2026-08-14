package com.andrewovens.weeklybudget2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DatesTest {

    private static Calendar at(TimeZone zone, int year, int month, int day, int hour) {
        Calendar c = new GregorianCalendar(zone);
        c.clear();
        c.set(year, month, day, hour, 0, 0);
        return c;
    }

    @Test
    public void countsWholeDaysForward() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        assertEquals(7, Dates.daysBetween(at(utc, 2026, Calendar.JANUARY, 1, 9),
                at(utc, 2026, Calendar.JANUARY, 8, 9)));
    }

    @Test
    public void countsWholeDaysBackward() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        assertEquals(-7, Dates.daysBetween(at(utc, 2026, Calendar.JANUARY, 8, 9),
                at(utc, 2026, Calendar.JANUARY, 1, 9)));
    }

    @Test
    public void ignoresTimeOfDay() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        // 23:00 to 01:00 the next day is two hours, but one calendar day.
        assertEquals(1, Dates.daysBetween(at(utc, 2026, Calendar.JANUARY, 1, 23),
                at(utc, 2026, Calendar.JANUARY, 2, 1)));
    }

    /**
     * The regression this method exists for: spanning the US spring-forward,
     * a month is 24h short of a whole number of days, and truncating integer
     * division reported one day fewer than it should.
     */
    @Test
    public void spansSpringForwardWithoutLosingADay() {
        TimeZone denver = TimeZone.getTimeZone("America/Denver");

        // 2026 DST in the US starts on 8 March.
        Calendar before = at(denver, 2026, Calendar.MARCH, 1, 12);
        Calendar after = at(denver, 2026, Calendar.MARCH, 31, 12);

        assertEquals(30, Dates.daysBetween(before, after));

        long millis = after.getTimeInMillis() - before.getTimeInMillis();
        assertEquals("the old millisecond division is what regressed",
                29, (int) (millis / (24L * 60 * 60 * 1000)));
    }

    @Test
    public void spansFallBackWithoutGainingADay() {
        TimeZone denver = TimeZone.getTimeZone("America/Denver");

        // 2026 DST in the US ends on 1 November.
        Calendar before = at(denver, 2026, Calendar.OCTOBER, 15, 12);
        Calendar after = at(denver, 2026, Calendar.NOVEMBER, 15, 12);

        assertEquals(31, Dates.daysBetween(before, after));
    }

    @Test
    public void isZeroForTheSameDay() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        assertEquals(0, Dates.daysBetween(at(utc, 2026, Calendar.JUNE, 10, 0),
                at(utc, 2026, Calendar.JUNE, 10, 23)));
    }
}
