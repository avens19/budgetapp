package com.andrewovens.weeklybudget2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.text.ParseException;
import java.util.*;

import org.json.*;

class API {
    private static final String baseUrl = BuildConfig.API_BASE_URL;

    /**
     * The header the server stamps its own clock into on the two change-feed
     * endpoints. Reading the watermark back from here is what keeps the sync
     * off the device clock.
     */
    static final String WATERMARK_HEADER = "X-Watermark";

    /**
     * A page of changes, plus the point in the server's clock that the page is
     * complete up to.
     *
     * <p>{@code watermark} is null when the server did not send the header —
     * an older deployment, or a proxy that stripped it. The caller must then
     * leave its stored watermark alone rather than guess: guessing is what the
     * device clock used to do.
     */
    static final class Page<T> {
        final List<T> items;
        @Nullable
        final String watermark;

        Page(List<T> items, @Nullable String watermark) {
            this.items = items;
            this.watermark = watermark;
        }
    }

    static Budget CreateBudget(@NonNull Budget b) throws JSONException, IOException {
        String urlString = baseUrl + "budget";
        URL url = new URL(urlString);

        JSONObject budget = b.toJson(true);
        String response = NetworkOperations.HttpPost(url, budget.toString());

        JSONObject responseBudget = new JSONObject(response);

        return Budget.fromJson(responseBudget);
    }

    static void EditBudget(@NonNull Budget b) throws Exception {
        String urlString = baseUrl + "budget/" + b.UniqueId;
        URL url = new URL(urlString);

        JSONObject budget = b.toJson(true);
        String response = NetworkOperations.HttpPost(url, budget.toString(), "PUT");

        if (!response.isEmpty())
            throw new Exception("Edit failed!");
    }

    static Budget GetBudget(String id) throws JSONException, IOException {
        String urlString = baseUrl + "budget/" + id;
        URL url = new URL(urlString);

        String response = NetworkOperations.HttpGet(url);

        JSONObject responseBudget = new JSONObject(response);

        return Budget.fromJson(responseBudget);
    }

    static Page<Expense> GetExpenses(String id, String watermarkString) throws IOException, JSONException, ParseException {
        String urlString = baseUrl + "budget/" + id + "/Expenses?watermark=" + encode(watermarkString);
        URL url = new URL(urlString);

        NetworkOperations.Response response = NetworkOperations.HttpGetWithHeaders(url);

        JSONArray responseArray = new JSONArray(response.body);

        List<Expense> expenses = new ArrayList<>();

        for (int i = 0; i < responseArray.length(); i++) {
            JSONObject jo = responseArray.getJSONObject(i);
            expenses.add(Expense.fromJson(jo));
        }

        return new Page<>(expenses, response.header(WATERMARK_HEADER));
    }

    static Page<Category> GetCategories(String id, String watermarkString) throws IOException, JSONException, ParseException {
        String urlString = baseUrl + "budget/" + id + "/Categories?watermark=" + encode(watermarkString);
        URL url = new URL(urlString);

        NetworkOperations.Response response = NetworkOperations.HttpGetWithHeaders(url);

        JSONArray responseArray = new JSONArray(response.body);

        List<Category> categories = new ArrayList<>();

        for (int i = 0; i < responseArray.length(); i++) {
            JSONObject jo = responseArray.getJSONObject(i);
            categories.add(Category.fromJson(jo));
        }

        return new Page<>(categories, response.header(WATERMARK_HEADER));
    }

    /**
     * The watermark is an ISO-8601 timestamp, whose ':' and '+' are reserved
     * in a query string. It used to be pasted in raw, which a strict proxy or
     * a future non-UTC offset would reject.
     */
    private static String encode(@Nullable String watermark) throws UnsupportedEncodingException {
        return watermark != null ? URLEncoder.encode(watermark, "UTF-8") : "";
    }

    static Expense AddExpense(@NonNull Expense e) throws JSONException, IOException, ParseException {
        String urlString = baseUrl + "expense";
        URL url = new URL(urlString);

        JSONObject expense = e.toJson();
        String response = NetworkOperations.HttpPost(url, expense.toString());

        JSONObject responseExpense = new JSONObject(response);

        return Expense.fromJson(responseExpense);
    }

    static Category AddCategory(Category c) throws JSONException, IOException, ParseException {
        String urlString = baseUrl + "categories";
        URL url = new URL(urlString);

        JSONObject expense = c.toJson();
        String response = NetworkOperations.HttpPost(url, expense.toString());

        JSONObject responseCategory = new JSONObject(response);

        return Category.fromJson(responseCategory);
    }

    static void EditExpense(Expense e) throws Exception {
        String urlString = baseUrl + "expense/" + e.Id;
        URL url = new URL(urlString);

        JSONObject expense = e.toJson();
        String response = NetworkOperations.HttpPost(url, expense.toString(), "PUT");

        if (!response.isEmpty())
            throw new Exception("Update failed!");
    }

    static void EditCategory(Category c) throws Exception {
        String urlString = baseUrl + "categories/" + c.Id;
        URL url = new URL(urlString);

        JSONObject category = c.toJson();
        String response = NetworkOperations.HttpPost(url, category.toString(), "PUT");

        if (!response.isEmpty())
            throw new Exception("Update failed!");
    }

    static void DeleteExpense(Expense e) throws JSONException, IOException, ParseException {
        String urlString = baseUrl + "expense/" + e.Id;
        URL url = new URL(urlString);

        String response = NetworkOperations.HttpGet(url, "DELETE");

        JSONObject responseExpense = new JSONObject(response);

        Expense.fromJson(responseExpense);
    }

    static Category DeleteCategory(Category c) throws JSONException, IOException, ParseException {
        String urlString = baseUrl + "categories/" + c.Id;
        URL url = new URL(urlString);

        String response = NetworkOperations.HttpGet(url, "DELETE");

        JSONObject responseCategory = new JSONObject(response);

        return Category.fromJson(responseCategory);
    }
}
