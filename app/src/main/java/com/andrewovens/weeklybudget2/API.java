package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.net.*;
import java.text.ParseException;
import java.util.*;

import org.json.*;

@SuppressLint("SimpleDateFormat")
public class API {
	private static String baseUrl = "http://budgetapp.azurewebsites.net/api/";
	
	public static Budget CreateBudget(Budget b) throws JSONException, IOException
	{
		String urlString = baseUrl + "budget";
		URL url = new URL(urlString);
		
		JSONObject budget = b.toJson(true);
		String response = NetworkOperations.HttpPost(url, budget.toString());
		
		JSONObject responseBudget = new JSONObject(response);
		
		return Budget.fromJson(responseBudget);
	}
	
	public static void EditBudget(Budget b) throws Exception
	{
		String urlString = baseUrl + "budget/" + b.UniqueId;
		URL url = new URL(urlString);
		
		JSONObject budget = b.toJson(true);
		String response = NetworkOperations.HttpPost(url, budget.toString(), "PUT");
		
		if(!response.isEmpty())
			throw new Exception("Edit failed!");
	}
	
	public static Budget GetBudget(String id) throws JSONException, IOException
	{
		String urlString = baseUrl + "budget/" + id;
		URL url = new URL(urlString);
		
		String response = NetworkOperations.HttpGet(url);
		
		JSONObject responseBudget = new JSONObject(response);
		
		return Budget.fromJson(responseBudget);
	}
	
	public static List<Expense> GetWeek(String id, int daysBackFromToday) throws IOException, JSONException, ParseException
	{
		Calendar calendar=Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
		Date d = calendar.getTime();
		String date = Dates.getDateString(d);
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
	
	public static List<Expense> GetExpenses(String id) throws IOException, JSONException, ParseException
	{
		String urlString = baseUrl + "budget/" + id + "/Expenses?watermark=null";
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
	
	public static List<Expense> GetExpenses(String id, String dateWatermark) throws IOException, JSONException, ParseException
	{
		String urlString = baseUrl + "budget/" + id + "/Expenses?watermark=" + dateWatermark;
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
	
	public static void EditExpense(Expense e) throws Exception
	{
		String urlString = baseUrl + "expense/" + e.Id;
		URL url = new URL(urlString);
		
		JSONObject expense = e.toJson();
		String response = NetworkOperations.HttpPost(url, expense.toString(), "PUT");
		
		if(!response.isEmpty())
			throw new Exception("Update failed!");
	}
	
	public static Expense DeleteExpense(Expense e) throws JSONException, IOException, ParseException
	{
		String urlString = baseUrl + "expense/" + e.Id;
		URL url = new URL(urlString);
		
		String response = NetworkOperations.HttpGet(url, "DELETE");
		
		JSONObject responseExpense = new JSONObject(response);
		
		return Expense.fromJson(responseExpense);
	}
}
