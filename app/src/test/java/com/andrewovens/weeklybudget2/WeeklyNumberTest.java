package com.andrewovens.weeklybudget2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class WeeklyNumberTest {

    private static final double EXACT = 0.0000001;

    @Test
    public void annualisesEachPeriod() {
        assertEquals(5200, WeeklyNumber.perYear("100", WeeklyNumber.Period.WEEKLY), EXACT);
        assertEquals(2600, WeeklyNumber.perYear("100", WeeklyNumber.Period.FORTNIGHTLY), EXACT);
        assertEquals(2400, WeeklyNumber.perYear("100", WeeklyNumber.Period.SEMI_MONTHLY), EXACT);
        assertEquals(1200, WeeklyNumber.perYear("100", WeeklyNumber.Period.MONTHLY), EXACT);
        assertEquals(100, WeeklyNumber.perYear("100", WeeklyNumber.Period.YEARLY), EXACT);
    }

    /**
     * The prompts are all optional, so most of them arrive empty. None of that
     * is an error; it is zero.
     */
    @Test
    public void treatsUnusableAmountsAsNothing() {
        assertEquals(0, WeeklyNumber.perYear("", WeeklyNumber.Period.MONTHLY), EXACT);
        assertEquals(0, WeeklyNumber.perYear("   ", WeeklyNumber.Period.MONTHLY), EXACT);
        assertEquals(0, WeeklyNumber.perYear(null, WeeklyNumber.Period.MONTHLY), EXACT);
        assertEquals(0, WeeklyNumber.perYear(".", WeeklyNumber.Period.MONTHLY), EXACT);
        assertEquals(0, WeeklyNumber.perYear("twelve", WeeklyNumber.Period.MONTHLY), EXACT);
    }

    /** Some keyboards give a comma for the decimal separator. */
    @Test
    public void readsACommaDecimalSeparator() {
        assertEquals(150, WeeklyNumber.perYear("12,50", WeeklyNumber.Period.MONTHLY), EXACT);
    }

    /**
     * Infinity parses happily out of "Infinity" and would poison every total
     * downstream, including the amount written to the budget.
     */
    @Test
    public void refusesValuesThatAreNotFinite() {
        assertEquals(0, WeeklyNumber.parse("Infinity"), EXACT);
        assertEquals(0, WeeklyNumber.parse("NaN"), EXACT);
    }

    /**
     * The headline sum. Pay every two weeks, bills monthly, insurance yearly:
     * the three cycles that make "a month is four weeks" wrong.
     */
    @Test
    public void dividesWhatIsLeftAcrossFiftyTwoWeeks() {
        double in = WeeklyNumber.perYear("2000", WeeklyNumber.Period.FORTNIGHTLY);
        double out = WeeklyNumber.perYear("1500", WeeklyNumber.Period.MONTHLY)
                + WeeklyNumber.perYear("1200", WeeklyNumber.Period.YEARLY);

        // 52,000 in, 19,200 out, 32,800 left over 52 weeks.
        assertEquals(52000, in, EXACT);
        assertEquals(19200, out, EXACT);
        assertEquals(630.769230, WeeklyNumber.weekly(in, out), 0.000001);
    }

    /**
     * Spending more than you earn is the finding the helper exists to surface,
     * so it comes back negative rather than floored at zero.
     */
    @Test
    public void reportsAShortfallAsNegative() {
        assertTrue(WeeklyNumber.weekly(12000, 24000) < 0);
    }

    @Test
    public void roundTripsAPlan() {
        String[] amounts = new String[WeeklyNumber.LINES.length];
        WeeklyNumber.Period[] periods = new WeeklyNumber.Period[WeeklyNumber.LINES.length];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = i == 0 ? "1234.56" : "";
            periods[i] = WeeklyNumber.LINES[i].period;
        }
        periods[1] = WeeklyNumber.Period.YEARLY;

        List<WeeklyNumber.Entry> read;
        try {
            read = WeeklyNumber.fromJson(WeeklyNumber.toJson(amounts, periods));
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        assertEquals(WeeklyNumber.LINES.length, read.size());
        assertEquals("1234.56", read.get(0).amount);
        assertEquals(WeeklyNumber.Period.YEARLY, read.get(1).period);
    }

    /**
     * A plan is stored positionally, so one written against a different set of
     * prompts has to be dropped rather than read off by the wrong labels.
     */
    @Test
    public void ignoresAPlanThatNoLongerFitsTheForm() {
        assertNull(WeeklyNumber.fromJson(null));
        assertNull(WeeklyNumber.fromJson("not json"));
        assertNull(WeeklyNumber.fromJson("[{\"amount\":\"10\",\"period\":\"MONTHLY\"}]"));
        assertNull(WeeklyNumber.fromJson("[{\"amount\":\"10\",\"period\":\"DAILY\"}]"));
    }
}
