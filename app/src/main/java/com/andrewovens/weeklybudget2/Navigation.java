package com.andrewovens.weeklybudget2;

import android.widget.ArrayAdapter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * The action-bar dropdown that switches between the five views.
 *
 * <p>All five screens set up an identical adapter, so it lives here once.
 *
 * <p>The list-navigation API is deprecated in favour of tabs or a navigation
 * drawer, but AppCompat still implements it and it is the app's established
 * navigation. Replacing it is a UI redesign, not a compatibility fix, so it is
 * kept as-is and the deprecation is suppressed in one place instead of five.
 */
@SuppressWarnings("deprecation")
final class Navigation {

    static final int WEEK = 0;
    static final int MONTH = 1;
    static final int CATEGORY_WEEK = 2;
    static final int CATEGORY_MONTH = 3;
    static final int CATEGORY = 4;

    private Navigation() {
    }

    /**
     * Installs the dropdown and selects {@code position} without notifying
     * {@code listener} of a move it did not ask for — every screen treats its
     * own index as a no-op, so selecting it is harmless.
     */
    static void setUp(AppCompatActivity activity, ActionBar.OnNavigationListener listener, int position) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(
                new ArrayAdapter<>(
                        actionBar.getThemedContext(),
                        R.layout.main_menu_item,
                        R.id.main_menu_item_text,
                        new String[]{
                                activity.getString(R.string.title_week),
                                activity.getString(R.string.title_month),
                                activity.getString(R.string.title_category_week),
                                activity.getString(R.string.title_category_month),
                                activity.getString(R.string.title_category),
                        }),
                listener);
        actionBar.setSelectedNavigationItem(position);
    }

    static void select(AppCompatActivity activity, int position) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSelectedNavigationItem(position);
        }
    }
}
