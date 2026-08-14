package com.andrewovens.weeklybudget2;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.*;

public class Expense {

    /**
     * The wire format the API uses. It is a fixed machine format, so it has to
     * be parsed and printed with {@link Locale#US} — under a locale with a
     * non-Gregorian calendar or non-ASCII digits (Thai, Hindi, Arabic) the
     * default locale produces dates the server cannot read.
     */
    private static final String WIRE_DATE_FORMAT = "yyyy-MM-dd";

    long Id;
    public Date Date;
    String Description;
    public double Amount;
    String BudgetId;
    Long CategoryId;
    String State;
    boolean IsDeleted;
    boolean IsSystem;

    public Expense() {
        IsSystem = false;
    }

    public static Expense fromJson(@NonNull JSONObject jo) throws ParseException, JSONException {
        Expense e = new Expense();
        e.Id = jo.getLong("Id");
        DateFormat formatter = new SimpleDateFormat(WIRE_DATE_FORMAT, Locale.US);
        e.Date = formatter.parse(jo.getString("Date"));
        e.Description = jo.getString("Description");
        e.Amount = jo.getDouble("Amount");
        e.BudgetId = jo.getString("BudgetId");
        long cat = jo.optLong("CategoryId", -1);
        e.CategoryId = cat != -1 ? cat : null;
        e.IsDeleted = jo.optBoolean("IsDeleted", false);
        e.IsSystem = jo.optBoolean("IsSystem", false);
        return e;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("Id", Id);
        DateFormat formatter = new SimpleDateFormat(WIRE_DATE_FORMAT, Locale.US);
        jo.put("Date", formatter.format(Date));
        jo.put("Description", Description);
        jo.put("Amount", Amount);
        jo.put("BudgetId", BudgetId);
        if (CategoryId != null) {
            jo.put("CategoryId", CategoryId);
        }
        jo.put("IsSystem", IsSystem);
        return jo;
    }

}
