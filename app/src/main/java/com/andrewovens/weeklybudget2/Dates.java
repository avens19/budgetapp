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
