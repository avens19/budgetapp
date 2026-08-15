package com.andrewovens.weeklybudget2;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * The bottom bar that switches between the app's three destinations.
 *
 * <p>This replaces the action bar's list-navigation dropdown, which was
 * deprecated, invisible until tapped, and listed all five screens flat. The
 * two category charts collapse into one destination — a segmented control on
 * that screen picks week or month — and "manage categories" moves to the
 * categories toolbar, which is where you look for it.
 *
 * <p>The five positions stay as they were: they are the wire format between
 * screens ({@link ScreenSwitcher} hands one back to {@link WeekActivity}), so
 * changing the navigation UI does not disturb the routing.
 */
final class Navigation {

    static final int WEEK = 0;
    static final int MONTH = 1;
    static final int CATEGORY_WEEK = 2;
    static final int CATEGORY_MONTH = 3;
    static final int CATEGORY = 4;

    interface OnDestinationSelected {
        /** @param position one of the constants above. */
        void onDestinationSelected(int position);
    }

    private Navigation() {
    }

    static void setUp(AppCompatActivity activity, final int ownPosition,
                      final OnDestinationSelected listener) {
        final BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav == null) {
            return;
        }

        // Selecting an item notifies the listener, so the initial selection is
        // made with no listener attached rather than relying on every caller
        // to no-op its own destination.
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(itemIdFor(ownPosition));

        nav.setOnItemSelectedListener(item -> {
            int target = positionFor(item.getItemId());

            // Both chart screens live under the Categories item, so tapping it
            // from either one is a no-op rather than a jump to the week chart.
            boolean onChartScreen = ownPosition == CATEGORY_WEEK || ownPosition == CATEGORY_MONTH;
            if (target == ownPosition || (onChartScreen && item.getItemId() == itemIdFor(ownPosition))) {
                return true;
            }

            listener.onDestinationSelected(target);
            return true;
        });
    }

    /** Re-selects {@code position} without firing the listener. */
    static void select(AppCompatActivity activity, int position) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav != null && nav.getSelectedItemId() != itemIdFor(position)) {
            nav.getMenu().findItem(itemIdFor(position)).setChecked(true);
        }
    }

    @IdRes
    private static int itemIdFor(int position) {
        if (position == MONTH) {
            return R.id.nav_month;
        }
        if (position == CATEGORY_WEEK || position == CATEGORY_MONTH || position == CATEGORY) {
            return R.id.nav_categories;
        }
        return R.id.nav_week;
    }

    private static int positionFor(@IdRes int itemId) {
        if (itemId == R.id.nav_month) {
            return MONTH;
        }
        if (itemId == R.id.nav_categories) {
            return CATEGORY_WEEK;
        }
        return WEEK;
    }
}
