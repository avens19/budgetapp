package com.andrewovens.weeklybudget2;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ExpenseTest {

    private final Locale original = Locale.getDefault();

    @After
    public void restoreLocale() {
        Locale.setDefault(original);
    }

    private static Expense sample() {
        Expense e = new Expense();
        e.Id = 42;
        e.Date = new GregorianCalendar(2026, Calendar.MARCH, 9).getTime();
        e.Description = "Coffee";
        e.Amount = 4.25;
        e.BudgetId = "budget-1";
        return e;
    }

    @Test
    public void writesTheWireDateFormat() throws Exception {
        Locale.setDefault(Locale.US);
        assertEquals("2026-03-09", sample().toJson().getString("Date"));
    }

    /**
     * The API's date format is machine-readable and fixed. Under a locale with
     * a non-Gregorian calendar it used to be serialised in that calendar's era
     * — a Thai device sent 2569 instead of 2026.
     */
    @Test
    public void writesTheWireDateFormatUnderANonGregorianLocale() throws Exception {
        Locale.setDefault(new Locale("th", "TH", "TH"));
        assertEquals("2026-03-09", sample().toJson().getString("Date"));
    }

    @Test
    public void roundTripsThroughJson() throws Exception {
        Locale.setDefault(new Locale("th", "TH", "TH"));

        JSONObject json = sample().toJson();
        Expense parsed = Expense.fromJson(json);

        assertEquals(sample().Date, parsed.Date);
        assertEquals("Coffee", parsed.Description);
        assertEquals(4.25, parsed.Amount, 0.0001);
        assertEquals("budget-1", parsed.BudgetId);
    }

    @Test
    public void treatsAMissingCategoryAsNull() throws Exception {
        Locale.setDefault(Locale.US);
        assertEquals(null, Expense.fromJson(sample().toJson()).CategoryId);
    }

    @Test
    public void keepsAPresentCategory() throws Exception {
        Locale.setDefault(Locale.US);

        Expense e = sample();
        e.CategoryId = 7L;

        assertEquals(Long.valueOf(7L), Expense.fromJson(e.toJson()).CategoryId);
    }
}
