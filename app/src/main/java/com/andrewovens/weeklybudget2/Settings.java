package com.andrewovens.weeklybudget2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private static final String SETTINGS_NAME = "WEEKLY_BUDGET_SETTINGS";
    private static final String BUDGET = "BUDGET";
    private static final String BUDGETS = "BUDGETS";
    private static final String CURRENT_ID = "CURRENTID";
    private static final String CURRENT_CATEGORY_ID = "CURRENTCATEGORYID";
    private static final String SEEN_TUTORIAL = "SEENTUTORIAL";
    private static final String DENSE_LAYOUT = "DENSELAYOUT";

    public static Budget getBudget(Context cxt) {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        String budgetString = settings.getString(BUDGET, null);

        if (budgetString == null)
            return null;

        try {
            return Budget.fromJson(new JSONObject(budgetString));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void setBudget(Context cxt, Budget b) throws JSONException {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BUDGET, b != null ? b.toJson(false).toString() : null);
        editor.apply();
    }

    /**
     * Makes a budget the current one and adds it to the list, replacing an
     * existing entry with the same id rather than appending a second.
     *
     * <p>Joining a budget the device already has is entirely normal — an invite
     * link tapped on the wrong phone, or a second tap on the same one — and
     * appending would put the same budget in the switcher twice with no way to
     * tell the copies apart.
     */
    static void rememberBudget(Context cxt, Budget budget) throws JSONException {
        setBudget(cxt, budget);

        Budget[] existing = getBudgets(cxt);
        if (existing == null) {
            setBudgets(cxt, new Budget[]{budget});
            return;
        }

        for (int i = 0; i < existing.length; i++) {
            if (existing[i].UniqueId.equals(budget.UniqueId)) {
                existing[i] = budget;
                setBudgets(cxt, existing);
                return;
            }
        }

        Budget[] grown = new Budget[existing.length + 1];
        System.arraycopy(existing, 0, grown, 0, existing.length);
        grown[existing.length] = budget;
        setBudgets(cxt, grown);
    }

    /**
     * Whether to draw the week compactly: no day headings, a one-line balance
     * summary instead of the hero card, and rows separated by rules rather than
     * spaced-out cards.
     *
     * <p>Asked for by long-time users of the pre-2026 layout, and the reasoning
     * in their words is worth keeping: the roomy design shows far fewer expenses
     * at once, and splitting by day works against people who use the week as a
     * single running list and do not always put an expense on the right day.
     *
     * <p>A display preference for this device, not part of the budget, so it does
     * not sync — two people sharing a budget can each have the layout they want.
     */
    static boolean isDenseLayout(Context cxt) {
        return cxt.getSharedPreferences(SETTINGS_NAME, 0).getBoolean(DENSE_LAYOUT, false);
    }

    static void setDenseLayout(Context cxt, boolean dense) {
        cxt.getSharedPreferences(SETTINGS_NAME, 0).edit().putBoolean(DENSE_LAYOUT, dense).apply();
    }

    static Budget[] getBudgets(Context cxt) {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        String budgetString = settings.getString(BUDGETS, null);

        if (budgetString == null)
            return null;

        try {
            JSONArray array = new JSONArray(budgetString);
            Budget[] bs = new Budget[array.length()];
            for (int i = 0; i < array.length(); i++) {
                bs[i] = Budget.fromJson(new JSONObject(array.getString(i)));
            }
            return bs;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void setBudgets(Context cxt, Budget[] bs) throws JSONException {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        if (bs != null) {
            JSONArray array = new JSONArray();
            for (Budget b : bs) {
                array.put(b.toJson(false).toString());
            }
            editor.putString(BUDGETS, array.toString());
        } else {
            editor.putString(BUDGETS, null);
        }
        editor.apply();
    }

    /**
     * Whether the "how this works" pages have been shown. Existing installs
     * have no flag, so they would see the tutorial once on upgrade; the caller
     * only asks on first run, when there is no budget yet, so they do not.
     */
    static boolean hasSeenTutorial(Context cxt) {
        return cxt.getSharedPreferences(SETTINGS_NAME, 0).getBoolean(SEEN_TUTORIAL, false);
    }

    static void setSeenTutorial(Context cxt) {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        settings.edit().putBoolean(SEEN_TUTORIAL, true).apply();
    }

    static long getNextId(Context cxt) {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        long id = settings.getLong(CURRENT_ID, 1000000000000L);
        setCurrentId(cxt, id + 1);
        return id;
    }

    private static void setCurrentId(Context cxt, long id) {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(CURRENT_ID, id);
        editor.apply();
    }

    static long getNextCategoryId(Context cxt) {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        long id = settings.getLong(CURRENT_CATEGORY_ID, 1000000000000L);
        setCurrentCategoryId(cxt, id + 1);
        return id;
    }

    private static void setCurrentCategoryId(Context cxt, long id) {
        SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(CURRENT_CATEGORY_ID, id);
        editor.apply();
    }
}
