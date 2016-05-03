package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.*;

@SuppressLint("SimpleDateFormat")
public class Expense {
	public long Id;
	public Date Date;
	public String Description;
	public double Amount;
	public String BudgetId;
	public Long CategoryId;
	public String State;
	public boolean IsDeleted;
	public boolean IsSystem;
	
	public Expense(){
        IsSystem = false;
    }
	
	public static Expense fromJson(JSONObject jo) throws ParseException, JSONException
	{
		Expense e = new Expense();
		e.Id = jo.getLong("Id");
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
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
	
	public JSONObject toJson() throws JSONException
	{
		JSONObject jo = new JSONObject();
		jo.put("Id", Id);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		jo.put("Date", formatter.format(Date));
		jo.put("Description", Description);
		jo.put("Amount", Amount);
		jo.put("BudgetId", BudgetId);
		if(CategoryId != null)
		{
			jo.put("CategoryId", CategoryId);
		}
        jo.put("IsSystem", IsSystem);
		return jo;
	}
	
}
