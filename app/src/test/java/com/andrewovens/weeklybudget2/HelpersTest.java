package com.andrewovens.weeklybudget2;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import java.util.Locale;

public class HelpersTest {

    private final Locale original = Locale.getDefault();

    @After
    public void restoreLocale() {
        Locale.setDefault(original);
    }

    @Test
    public void formatsTwoDecimalsForATwoDecimalCurrency() {
        Locale.setDefault(Locale.US);
        assertEquals("12.50", Helpers.doubleString(12.5));
    }

    /**
     * The value goes straight back into {@code Double.parseDouble}, so it has
     * to keep an ASCII '.' separator even where the locale would use a comma.
     */
    @Test
    public void usesADotSeparatorUnderACommaLocale() {
        Locale.setDefault(Locale.GERMANY);
        String formatted = Helpers.doubleString(12.5);

        assertEquals("12.50", formatted);
        assertEquals(12.5, Double.parseDouble(formatted), 0.0001);
    }

    /**
     * Locales that render digits outside ASCII used to produce a string
     * {@code Double.parseDouble} could not read back.
     */
    @Test
    public void usesAsciiDigitsUnderANonAsciiDigitLocale() {
        Locale.setDefault(new Locale("hi", "IN"));
        String formatted = Helpers.doubleString(1234.5);

        assertEquals(1234.5, Double.parseDouble(formatted), 0.0001);
    }

    @Test
    public void dropsDecimalsForAZeroDecimalCurrency() {
        Locale.setDefault(Locale.JAPAN);
        assertEquals("1234", Helpers.doubleString(1234.4));
    }

    /**
     * A language-only locale has no country, so it resolves to the "no
     * currency" code XXX whose default fraction digits are -1. Treating that
     * as zero would round the cents off every amount.
     */
    @Test
    public void keepsCentsWhenTheLocaleHasNoCurrency() {
        Locale.setDefault(new Locale("en"));
        assertEquals("12.50", Helpers.doubleString(12.5));
    }

    @Test
    public void keepsCentsUnderTheRootLocale() {
        Locale.setDefault(Locale.ROOT);
        assertEquals("12.50", Helpers.doubleString(12.5));
    }
}
