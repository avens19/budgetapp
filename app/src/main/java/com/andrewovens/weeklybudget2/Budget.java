package com.andrewovens.weeklybudget2;

import android.content.Context;

import java.util.*;

import org.json.*;

public class Budget {
    public String Name;
    public String UniqueId;
    public double Amount;
    int StartDay;
    String Watermark;

    public Budget(boolean newBudget) {
        if (newBudget) {
            UUID uniqueId = UUID.randomUUID();
            UniqueId = uniqueId.toString();
        }
    }

    public JSONObject toJson(boolean forServer) throws JSONException {
        JSONObject jo = new JSONObject();
        if (Name != null)
            jo.put("Name", Name);
        jo.put("UniqueId", UniqueId);
        jo.put("StartDay", StartDay);
        jo.put("Amount", Amount);

        if (!forServer)
            jo.put("Watermark", Watermark);

        return jo;
    }

    public static Budget fromJson(JSONObject json) throws JSONException {
        Budget b = new Budget(false);
        b.Name = json.optString("Name");
        b.UniqueId = json.getString("UniqueId");
        b.StartDay = json.getInt("StartDay");
        b.Amount = json.getDouble("Amount");
        b.Watermark = json.optString("Watermark");
        return b;
    }

    static Budget update(Budget original, Budget updated) {
        if (original == null)
            return updated;

        original.Amount = updated.Amount;
        original.Name = updated.Name;
        original.StartDay = updated.StartDay;
        original.UniqueId = updated.UniqueId;
        if (updated.Watermark != null)
            original.Watermark = updated.Watermark;

        return original;
    }

    static void updateStoredBudget(Context c, Budget b) {
        try {
            Settings.setBudget(c, b);

            Budget[] budgets = Settings.getBudgets(c);
            Budget[] newBudgets;
            if (budgets != null) {
                newBudgets = new Budget[budgets.length];
                for (int i = 0; i < budgets.length; i++) {
                    if (budgets[i].UniqueId.equals(b.UniqueId)) {
                        newBudgets[i] = b;
                    } else {
                        newBudgets[i] = budgets[i];
                    }
                }
            } else {
                newBudgets = new Budget[1];
                newBudgets[0] = b;
            }

            Settings.setBudgets(c, newBudgets);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
