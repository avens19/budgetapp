package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

/**
 * Suppresses the activity transition for moves between bottom-bar
 * destinations.
 *
 * <p>Week, Month and Categories are separate activities, so the platform
 * animates each switch as a push — the whole screen slides in from the side.
 * That reads as "you have gone deeper", which is wrong: tabs are peers.
 *
 * <p>The two directions need different mechanisms, which is easy to get wrong:
 *
 * <ul>
 *   <li><b>Opening</b> is a property of the <em>intent</em>. It is tempting to
 *       reach for {@code overrideActivityTransition(OVERRIDE_TRANSITION_OPEN)}
 *       after {@code startActivity}, but that sets the animation for the
 *       activity it is called on being opened — the caller, which is not the
 *       one appearing — so it does nothing here. The flag is what targets the
 *       activity being launched, and it works on every API level.
 *   <li><b>Closing</b> is a property of the activity going away, and it has to
 *       be registered before the transition is computed rather than after
 *       {@code finish()}, so it is set in {@code onCreate}.
 * </ul>
 *
 * <p>Only tab switches use this. Opening the add-expense form or the tutorial
 * keeps its animation, because those really are a level down.
 */
final class Transitions {

    private Transitions() {
    }

    /** Launches {@code intent} without the incoming activity sliding in. */
    static Intent noAnimation(Intent intent) {
        return intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }

    /**
     * Registers "do not animate me away" for a screen that finishes itself on
     * a tab switch. Call from {@code onCreate}.
     */
    static void disableCloseAnimation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0);
        }
    }

    /**
     * The pre-API-34 half of the above: there, the close animation is a
     * pending property set immediately after {@code finish()}.
     */
    @SuppressWarnings("deprecation")
    static void afterFinish(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overridePendingTransition(0, 0);
        }
    }
}
