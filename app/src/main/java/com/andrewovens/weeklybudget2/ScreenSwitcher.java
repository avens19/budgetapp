package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

/**
 * The navigation and overflow-menu behaviour shared by the four secondary
 * screens (month, categories by week, categories by month, manage categories).
 *
 * <p>Only {@link WeekActivity} owns the back stack: the others hand a
 * {@code GOTO_*} code back to it and finish, so switching views never stacks
 * screens on top of each other.
 */
final class ScreenSwitcher {

    private static final int EDIT_BUDGET = 1;
    private static final int SWITCH_BUDGET = 2;

    private ScreenSwitcher() {
    }

    /** Routes a bottom-bar selection back through {@link WeekActivity}. */
    static void goToPosition(Activity activity, int position) {
        Intent i = new Intent(activity, WeekActivity.class);
        i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_WEEK + position);
        activity.setResult(Activity.RESULT_OK, i);
        activity.finish();
    }

    /**
     * Handles the shared overflow items. Returns false when the item is not
     * one of them so the caller can fall through to {@code super}.
     */
    static boolean onOptionsItemSelected(AppCompatActivity activity, MenuItem item, Budget budget) {
        int id = item.getItemId();

        if (id == R.id.action_current_budget) {
            if (budget != null) {
                activity.startActivityForResult(new Intent(activity, SwitchBudgetActivity.class), SWITCH_BUDGET);
            }
            return true;
        }

        if (id == R.id.action_settings) {
            if (budget != null) {
                try {
                    Intent i = new Intent(activity, NewBudgetActivity.class);
                    i.putExtra("budget", budget.toJson(false).toString());
                    activity.startActivityForResult(i, EDIT_BUDGET);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        if (id == R.id.action_edit_categories) {
            if (budget != null) {
                goToPosition(activity, Navigation.CATEGORY);
            }
            return true;
        }

        return false;
    }
}
