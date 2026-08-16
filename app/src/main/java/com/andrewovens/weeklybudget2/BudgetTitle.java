package com.andrewovens.weeklybudget2;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Keeps the budget's name on screen everywhere.
 *
 * <p>With more than one budget on a device it is otherwise possible to add a
 * week of expenses to the wrong one and have nothing on the page say so.
 *
 * <p>On the three bottom-bar screens the bar already says which screen you are
 * on, so the name takes the title outright. On a detail screen the title is
 * needed for the screen itself, so the name goes underneath it.
 */
final class BudgetTitle {

    private BudgetTitle() {
    }

    /** Main screens: the name is the title. */
    static void asTitle(AppCompatActivity activity, @Nullable Budget budget) {
        activity.setTitle(nameOf(activity, budget));
    }

    /** Detail screens: {@code title} stays, the name goes below it. */
    static void asSubtitle(AppCompatActivity activity, @StringRes int title,
                           @Nullable Budget budget) {
        activity.setTitle(title);
        ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setSubtitle(nameOf(activity, budget));
        }
    }

    private static String nameOf(AppCompatActivity activity, @Nullable Budget budget) {
        return budget != null && budget.Name != null && !budget.Name.isEmpty()
                ? budget.Name
                : activity.getString(R.string.app_name);
    }
}
