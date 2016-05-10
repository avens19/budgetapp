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

    public static List<Expense> GetExpenses(String id, String watermarkString) throws IOException, JSONException, ParseException
    {
        String urlString = baseUrl + "budget/" + id + "/Expenses?watermark=" + watermarkString;
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

	public static List<Category> GetCategories(String id, String watermarkString) throws IOException, JSONException, ParseException
	{
		String urlString = baseUrl + "budget/" + id + "/Categories?watermark=" + watermarkString;
		URL url = new URL(urlString);

		String response = NetworkOperations.HttpGet(url);

		JSONArray responseArray = new JSONArray(response);

		List<Category> categories = new ArrayList<Category>();

		for(int i = 0; i < responseArray.length(); i++)
		{
			JSONObject jo = responseArray.getJSONObject(i);
			categories.add(Category.fromJson(jo));
		}

		return categories;
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

	public static Category AddCategory(Category c) throws JSONException, IOException, ParseException
	{
		String urlString = baseUrl + "categories";
		URL url = new URL(urlString);

		JSONObject expense = c.toJson();
		String response = NetworkOperations.HttpPost(url, expense.toString());

		JSONObject responseCategory = new JSONObject(response);

		return Category.fromJson(responseCategory);
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

    public static void EditCategory(Category c) throws Exception
    {
        String urlString = baseUrl + "categories/" + c.Id;
        URL url = new URL(urlString);

        JSONObject category = c.toJson();
        String response = NetworkOperations.HttpPost(url, category.toString(), "PUT");

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

	public static Category DeleteCategory(Category c) throws JSONException, IOException, ParseException
	{
		String urlString = baseUrl + "categories/" + c.Id;
		URL url = new URL(urlString);

		String response = NetworkOperations.HttpGet(url, "DELETE");

		JSONObject responseCategory = new JSONObject(response);

		return Category.fromJson(responseCategory);
	}
}
