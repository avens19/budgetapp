package com.andrewovens.weeklybudget;

import android.annotation.SuppressLint;
import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

@SuppressLint("SimpleDateFormat")
public class API {
	private static String baseUrl = "http://budgetapp.azurewebsites.net/api/";
	
	public static Budget CreateBudget(Budget b) throws JSONException, IOException
	{
		String urlString = baseUrl + "budget";
		URL url = new URL(urlString);
		
		JSONObject budget = b.toJson();
		String response = NetworkOperations.HttpPost(url, budget.toString());
		
		JSONObject responseBudget = new JSONObject(response);
		
		return Budget.fromJson(responseBudget);
	}
	
	public static Budget GetBudget(String id) throws JSONException, IOException
	{
		String urlString = baseUrl + "budget/" + id;
		URL url = new URL(urlString);
		
		String response = NetworkOperations.HttpGet(url);
		
		JSONObject responseBudget = new JSONObject(response);
		
		return Budget.fromJson(responseBudget);
	}
	
	public static List<Expense> GetWeek(String id) throws IOException, JSONException, ParseException
	{
		String date = getDateString(new Date());
		String urlString = baseUrl + "budget/" + id + "/Week/" + date;
		URL url = new URL(urlString);
		
		String response = NetworkOperations.HttpGet(url);
		
		JSONArray responseArray = new JSONArray(response);
		
		List<Expense> expenses = new ArrayList<Expense>();
		
		for(int i = 0; i < responseArray.length(); i++)
		{
			JSONObject jo = responseArray.getJSONObject(i);
			expenses.add(Expense.fromJson(jo));
		}
		
		return expenses;
	}
	
	public static Expense AddExpense(Expense e) throws JSONException, IOException, ParseException
	{
		String urlString = baseUrl + "expense";
		URL url = new URL(urlString);
		
		JSONObject expense = e.toJson();
		String response = NetworkOperations.HttpPost(url, expense.toString());
		
		JSONObject responseExpense = new JSONObject(response);
		
		return Expense.fromJson(responseExpense);
	}
	
	public static String getDateString(Date date)
	{
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		return formatter.format(date);
	}
	
	public static String getWeekDay(Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
		return sdf.format(date);
	}
}
