package com.andrewovens.weeklybudget2;

import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * "Jump to a date" for the period screens.
 *
 * <p>Stepping a week at a time is fine for last week and useless for last
 * March, so tapping the period heading opens a picker. The screens navigate by
 * "days back from today", so a chosen date is converted straight into that.
 */
final class PeriodPicker {

    interface OnPeriodChosen {
        /** @param daysBackFromToday positive for the past, negative for the future. */
        void onPeriodChosen(int daysBackFromToday);
    }

    private static final String TAG = "period_picker";

    private PeriodPicker() {
    }

    static void show(FragmentActivity activity, @StringRes int title, int currentDaysBack,
                     final boolean clampToPast, final OnPeriodChosen listener) {
        Calendar showing = Calendar.getInstance();
        showing.add(Calendar.DAY_OF_YEAR, -currentDaysBack);

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setSelection(toUtcMillis(showing))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar chosen = fromUtcMillis(selection);
            int daysBack = Dates.daysBetween(chosen, Calendar.getInstance());
            if (clampToPast && daysBack < 0) {
                daysBack = 0;
            }
            listener.onPeriodChosen(daysBack);
        });

        picker.show(activity.getSupportFragmentManager(), TAG);
    }

    /**
     * The picker works in UTC and hands back UTC midnight, so the local
     * calendar date has to be converted in both directions or a date chosen
     * west of Greenwich lands on the previous day.
     */
    private static long toUtcMillis(Calendar local) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    private static Calendar fromUtcMillis(long millis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(millis);

        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local;
    }
}
