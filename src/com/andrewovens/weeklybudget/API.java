package com.andrewovens.weeklybudget;

import java.io.IOException;
import java.net.*;

import org.json.*;

public class API {
	private static String baseUrl = "https://budgetapp.azurewebsites.com/api/";
	
	public static Budget CreateBudget(Budget b) throws JSONException, IOException
	{
		String urlString = baseUrl + "budget";
		URL url = new URL(urlString);
		
		JSONObject budget = b.toJson();
		String response = NetworkOperations.HttpPost(url, budget.toString(), "POST");
		
		JSONObject responseBudget = new JSONObject(response);
		
		return Budget.fromJson(responseBudget);
	}
}
