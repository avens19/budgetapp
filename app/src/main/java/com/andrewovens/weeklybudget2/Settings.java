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
