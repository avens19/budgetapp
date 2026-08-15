package com.andrewovens.weeklybudget2;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

class Dates {

    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    /**
     * A machine key for "which calendar day is this", used to group expenses.
     * Fixed format, so {@link Locale#US} — a locale with a non-Gregorian
     * calendar would otherwise group by the wrong day boundaries.
     */
    private static final String DAY_KEY_FORMAT = "yyyy-MM-dd";

    static String getWeekDay(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
        return sdf.format(date);
    }

    static String getLongWeekDay(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("E", Locale.getDefault());
        return sdf.format(date);
    }

    static String getDayOfMonth(Date date) {
        DateFormat formatter = new SimpleDateFormat("d", Locale.getDefault());
        return formatter.format(date);
    }

    static String getShortDateString(Context c, Date date) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR;
        return DateUtils.formatDateTime(c, date.getTime(), flags);
    }

    /** The date spelled out for a form field, e.g. "Saturday, 15 Aug". */
    static String getFullDateString(Context c, Date date) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY
                | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR;
        return DateUtils.formatDateTime(c, date.getTime(), flags);
    }

    static String dayKey(Date date) {
        return new SimpleDateFormat(DAY_KEY_FORMAT, Locale.US).format(date);
    }

    /**
     * Turns a {@link #dayKey} back into a heading. Today and yesterday are
     * named rather than dated, because that is how you think about the current
     * week.
     */
    static String formatDayKey(Context c, String key) {
        Date date;
        try {
            date = new SimpleDateFormat(DAY_KEY_FORMAT, Locale.US).parse(key);
        } catch (java.text.ParseException e) {
            return key;
        }
        if (date == null) {
            return key;
        }

        Calendar day = Calendar.getInstance();
        day.setTime(date);
        int daysAgo = daysBetween(day, Calendar.getInstance());

        // Today / Yesterday / Tomorrow, localised by the platform.
        if (daysAgo >= -1 && daysAgo <= 1) {
            return DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        }

        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY
                | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR;
        return DateUtils.formatDateTime(c, date.getTime(), flags);
    }

    /**
     * Whole calendar days from {@code from} to {@code to}, ignoring the time of
     * day.
     *
     * <p>The screens navigate by "days back from today", which used to be
     * derived by dividing a raw millisecond difference by 24 hours. Across a
     * daylight-saving boundary a span of N days is only {@code N * 24h - 1h}
     * long, so the integer division truncated to N-1 and navigation slipped a
     * day — enough to land on the wrong month at a month boundary. Zeroing the
     * time of day and rounding removes that.
     */
    static int daysBetween(Calendar from, Calendar to) {
        long diff = atStartOfDay(to) - atStartOfDay(from);
        return (int) Math.round(diff / (double) MILLIS_PER_DAY);
    }

    private static long atStartOfDay(Calendar calendar) {
        Calendar copy = (Calendar) calendar.clone();
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy.getTimeInMillis();
    }
}
