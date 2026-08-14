package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class Helpers {
    static void showNetworkErrorToastOnUi(final Activity a, final int resourceId) {
        a.runOnUiThread(() -> Toast.makeText(a, resourceId, Toast.LENGTH_SHORT).show());
    }

    /**
     * Formats an amount for an editable field: no currency symbol, but the
     * number of decimal places the current currency actually uses.
     *
     * <p>The digits and separator are deliberately pinned to {@link Locale#US}
     * because the value goes straight back into {@code Double.parseDouble},
     * which only accepts ASCII digits and a {@code '.'} separator.
     */
    static String doubleString(double d) {
        DecimalFormat df = new DecimalFormat(decimalPattern(), DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(d);
    }

    private static final int DEFAULT_FRACTION_DIGITS = 2;

    private static String decimalPattern() {
        Currency currency = NumberFormat.getCurrencyInstance().getCurrency();

        // A locale with no country (a bare "en", or Locale.ROOT) resolves to
        // the "no currency" code XXX, whose getDefaultFractionDigits() is -1;
        // some locales have no currency object at all. Either way, rounding an
        // expense to whole units would quietly drop the cents, so fall back to
        // two decimals. Zero is a real answer, though — JPY has no minor unit.
        int digits = currency != null ? currency.getDefaultFractionDigits() : -1;
        if (digits < 0) {
            digits = DEFAULT_FRACTION_DIGITS;
        }

        if (digits == 0) {
            return "0";
        }
        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < digits; i++) {
            pattern.append('0');
        }
        return pattern.toString();
    }

    static String currencyString(double d) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        return nf.format(d);
    }
}
