package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
     * Hands a URL to whatever the device uses for web pages.
     *
     * <p>A device with no browser at all is unusual but not impossible — a
     * stripped ROM, or a work profile that blocks it — and an uncaught
     * {@link ActivityNotFoundException} would take the whole screen down for
     * what is only a link.
     */
    static void openUrl(Activity a, String url) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, R.string.error_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the Play listing, which is where a review is left.
     *
     * <p>{@code market://} goes straight to the Play app rather than through a
     * browser and a redirect; a device without Play — a de-Googled ROM, or a
     * sideloaded build — has nothing to answer it, so the same listing's https
     * URL is the fallback.
     */
    static void openPlayListing(Activity a) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(a.getString(R.string.url_review_app))));
        } catch (ActivityNotFoundException e) {
            openUrl(a, a.getString(R.string.url_review));
        }
    }

    /**
     * Opens a mail composer addressed to {@code address}.
     *
     * <p>{@code ACTION_SENDTO} with a {@code mailto:} URI rather than
     * {@code ACTION_SEND}: the latter offers every app that can share text,
     * which on most phones means a chooser full of chat apps and no way to
     * send an email at all.
     */
    static void sendEmail(Activity a, String address, String subject) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address));
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        try {
            a.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, R.string.error_no_email, Toast.LENGTH_SHORT).show();
        }
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
