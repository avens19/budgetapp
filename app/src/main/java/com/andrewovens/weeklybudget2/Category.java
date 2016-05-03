package com.andrewovens.weeklybudget2;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by andrew on 01/05/16.
 */
public class Category {
    public long Id;
    public String Name;
    public String BudgetId;
    public String State;
    public boolean IsDeleted;

    public Category(){}

    public Category(String name, String budgetId)
    {
        Name = name;
        BudgetId = budgetId;
    }

    public static Category fromJson(JSONObject jo) throws ParseException, JSONException
    {
        Category c = new Category();
        c.Id = jo.getLong("Id");
        c.Name = jo.getString("Name");
        c.BudgetId = jo.getString("BudgetId");
        c.IsDeleted = jo.optBoolean("IsDeleted", false);
        return c;
    }

    public JSONObject toJson() throws JSONException
    {
        JSONObject jo = new JSONObject();
        jo.put("Id", Id);
        jo.put("Name", Name);
        jo.put("BudgetId", BudgetId);
        jo.put("IsDeleted", IsDeleted);
        return jo;
    }

    @Override
    public String toString()
    {
        return Name;
    }
}
