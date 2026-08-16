package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.os.Build;

/**
 * Suppresses the activity transition for moves between bottom-bar
 * destinations.
 *
 * <p>Week, Month and Categories are separate activities, so the platform
 * animates each switch as a push — the whole screen slides in from the side.
 * That reads as "you have gone deeper", which is wrong: tabs are peers, and
 * every other app with a bottom bar cross-fades or does nothing at all.
 *
 * <p>Only tab switches use this. Opening the add-expense form or the tutorial
 * keeps its animation, because those really are a level down.
 */
final class Transitions {

    private Transitions() {
    }

    /** Call immediately after {@code startActivity}. */
    static void suppressOpen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0);
        } else {
            legacy(activity);
        }
    }

    /** Call immediately after {@code finish}. */
    static void suppressClose(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0);
        } else {
            legacy(activity);
        }
    }

    /**
     * {@code overridePendingTransition} is deprecated from API 34, where
     * {@code overrideActivityTransition} replaces it, but it is still the only
     * option below that.
     */
    @SuppressWarnings("deprecation")
    private static void legacy(Activity activity) {
        activity.overridePendingTransition(0, 0);
    }
}
