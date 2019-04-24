package com.andrewovens.weeklybudget2;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by andrew on 01/05/16.
 */
public class Category {
    long Id;
    public String Name;
    String BudgetId;
    String State;
    boolean IsDeleted;

    Category() {
    }

    Category(String name, String budgetId) {
        Name = name;
        BudgetId = budgetId;
    }

    public static Category fromJson(JSONObject jo) throws JSONException {
        Category c = new Category();
        c.Id = jo.getLong("Id");
        c.Name = jo.getString("Name");
        c.BudgetId = jo.getString("BudgetId");
        c.IsDeleted = jo.optBoolean("IsDeleted", false);
        return c;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("Id", Id);
        jo.put("Name", Name);
        jo.put("BudgetId", BudgetId);
        jo.put("IsDeleted", IsDeleted);
        return jo;
    }

    @NonNull
    @Override
    public String toString() {
        return Name;
    }
}
