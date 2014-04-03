package com.andrewovens.weeklybudget;

import java.util.*;

import org.json.*;

public class Budget {
	public String UniqueId;
	public int StartDay;
	public double Amount;
	
	public Budget(boolean newBudget)
	{
		if(newBudget)
		{
			UUID uniqueId = UUID.randomUUID();
			UniqueId = uniqueId.toString();
		}
	}
	
	public JSONObject toJson() throws JSONException
	{
		JSONObject jo = new JSONObject();
		jo.put("UniqueId", UniqueId);
		jo.put("StartDay", StartDay);
		jo.put("Amount", Amount);
		return jo;
	}
	
	public static Budget fromJson(JSONObject json) throws JSONException
	{
		Budget b = new Budget(false);
		b.UniqueId = json.getString("UniqueId");
		b.StartDay = json.getInt("StartDay");
		b.Amount = json.getDouble("Amount");
		return b;
	}
}
