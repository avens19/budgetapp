package com.andrewovens.weeklybudget2;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Dates {

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
}
