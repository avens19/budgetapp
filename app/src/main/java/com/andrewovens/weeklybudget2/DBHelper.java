package com.andrewovens.weeklybudget2;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBHelper{

	public static final String DBNAME = "BUDGETDB";
	public static final String EXPENSESTABLENAME = "Expenses";
	public static final String CATEGORIESTABLENAME = "Categories";
    public static final String CREATEDSTATEKEY = "created";
    public static final String EDITEDSTATEKEY = "edited";
    public static final String DELETEDSTATEKEY = "deleted";
    public static final String SYNCEDSTATEKEY = "synced";
	private static SQLiteDatabase myDB;
    private static final Object locker = new Object();
	
	/* Expense State column
	 * 
	 * created - created but not synced
	 * edited - edited but not synced
	 * deleted - deleted but not synced
	 * synced - synced to DB
	 */

	public static void OpenDB(Context c)
	{
        synchronized (locker) {
            if (myDB == null || !myDB.isOpen())
                myDB = c.openOrCreateDatabase(DBNAME, SQLiteDatabase.OPEN_READWRITE, null);

            CreateExpensesTable();

            try {
                if (myDB.needUpgrade(1)) {
                    CreateCategoriesTable();
                    AddCategoryFK();
                    myDB.setVersion(1);
                }
                if (myDB.needUpgrade(2)) {
                    AddIsSystem();
                    myDB.setVersion(2);
                }
            } catch (Exception e) {
                myDB.setVersion(0);
            }
        }
	}

	public static void CreateExpensesTable()
	{
		myDB.execSQL(
				"CREATE TABLE IF NOT EXISTS "+
						EXPENSESTABLENAME +
						"(Id int PRIMARY KEY, "+
						"Date text, "+
						"Description text, "+
						"Amount real, "+
						"BudgetId text," +
						"State text)"
				);
	}

	public static void CreateCategoriesTable()
	{
		myDB.execSQL(
				"CREATE TABLE IF NOT EXISTS "+
						CATEGORIESTABLENAME +
						"(Id int PRIMARY KEY, "+
						"Name text, "+
						"BudgetId text," +
						"State text, "+
                        "IsDeleted int)"
		);
	}

	public static void AddCategoryFK()
	{
        myDB.execSQL("ALTER TABLE " + EXPENSESTABLENAME + " ADD COLUMN CategoryId int");
	}

    public static void AddIsSystem()
    {
        myDB.execSQL("ALTER TABLE " + EXPENSESTABLENAME + " ADD COLUMN IsSystem int");
        myDB.execSQL("UPDATE " + EXPENSESTABLENAME + " SET IsSystem = 0");
    }

	public static void AddExpense(Expense e, String state)
	{
		ContentValues cv = addExpenseValues(e, state);

		myDB.insertWithOnConflict(EXPENSESTABLENAME, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
	}

    private static ContentValues addExpenseValues(Expense e, String state)
    {
        ContentValues cv = new ContentValues();

        cv.put("Id", e.Id);
        cv.put("Date", getDateString(e.Date));
        cv.put("Description", e.Description);
        cv.put("Amount", e.Amount);
        cv.put("BudgetId", e.BudgetId);
        cv.put("CategoryId", e.CategoryId);
        cv.put("State", state);
        cv.put("IsSystem", e.IsSystem ? 1 : 0);

        return cv;
    }

    public static void AddCategory(Category c, String state)
    {
        ContentValues cv = addCategoryValues(c, state);

        myDB.insertWithOnConflict(CATEGORIESTABLENAME, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static ContentValues addCategoryValues(Category c, String state)
    {
        ContentValues cv = new ContentValues();

        cv.put("Id", c.Id);
        cv.put("Name", c.Name);
        cv.put("BudgetId", c.BudgetId);
        cv.put("State", state);
        cv.put("IsDeleted", c.IsDeleted ? 1 : 0);

        return cv;
    }
	
	public static void EditExpense(Expense e, String state)
	{
		ContentValues cv = addExpenseValues(e, state);
		
		String where = "Id = ?";
		String[] whereArgs = new String[]{Long.toString(e.Id)};

		myDB.update(EXPENSESTABLENAME, cv, where, whereArgs);
	}

    public static void EditCategory(Category c, String state)
    {
        ContentValues cv = addCategoryValues(c, state);

        String where = "Id = ?";
        String[] whereArgs = new String[]{Long.toString(c.Id)};

        myDB.update(CATEGORIESTABLENAME, cv, where, whereArgs);
    }
	
	public static void DeleteExpense(Expense e)
	{
		String where = "Id = ?";
		String[] whereArgs = new String[]{Long.toString(e.Id)};
		
		myDB.delete(EXPENSESTABLENAME, where, whereArgs);		
	}

    private static void DeleteCategory(Category c)
    {
        String where = "Id = ?";
        String[] whereArgs = new String[]{Long.toString(c.Id)};

        myDB.delete(CATEGORIESTABLENAME, where, whereArgs);
    }

    public static void ReplaceCategory(Category oldCategory, Category newCategory, String state)
    {
        DeleteCategory(oldCategory);
        AddCategory(newCategory, state);
        ContentValues cv = new ContentValues();

        cv.put("CategoryId", newCategory.Id);

        String where = "CategoryId = ?";
        String[] whereArgs = new String[]{Long.toString(oldCategory.Id)};

        myDB.update(EXPENSESTABLENAME, cv, where, whereArgs);
    }
	
	public static Expense GetExpense(long id)
	{
        String where = "Id = ?";
        String[] whereArgs = new String[]{Long.toString(id)};

		return queryExpenses(where, whereArgs, null).get(0);
	}

    private static List<Expense> queryExpenses(String where, String[] whereArgs, String orderBy){
        String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "CategoryId", "State", "IsSystem"};

        Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs, null, null, orderBy);

        List<Expense> list = new ArrayList<Expense>();

        c.moveToFirst();

        while(!c.isAfterLast())
        {
            Expense e = new Expense();
            e.Id = c.getLong(0);
            e.Date = getDate(c.getString(1));
            e.Description = c.getString(2);
            e.Amount = c.getDouble(3);
            e.BudgetId = c.getString(4);
            e.CategoryId = !c.isNull(5) ? c.getLong(5) : null;
            e.State = c.getString(6);
            e.IsSystem = c.getInt(7) == 1;
            list.add(e);

            c.moveToNext();
        }

        c.close();

        return list;
    }

    public static List<Category> GetActiveCategories(String budgetId, Long categoryId)
    {
        String where;
        String[] whereArgs;
        if(categoryId != null)
        {
            where = "BudgetId = ? AND (IsDeleted = ? OR Id = ?)";
            whereArgs = new String[]{budgetId, "0", Long.toString(categoryId)};
        }
        else
        {
            where = "BudgetId = ? AND IsDeleted = ?";
            whereArgs = new String[]{budgetId, "0"};
        }
        String orderBy = "Name";

        return queryCategories(where, whereArgs, orderBy);
    }

    private static List<Category> queryCategories(String where, String[] whereArgs, String orderBy)
    {
        String[] columns = new String[]{"Id", "Name", "BudgetId", "State", "IsDeleted"};

        Cursor cur = myDB.query(CATEGORIESTABLENAME, columns, where, whereArgs,null,null, orderBy);

        List<Category> list = new ArrayList<Category>();

        cur.moveToFirst();

        while(!cur.isAfterLast())
        {
            Category c = new Category();
            c.Id = cur.getLong(0);
            c.Name = cur.getString(1);
            c.BudgetId = cur.getString(2);
            c.State = cur.getString(3);
            c.IsDeleted = cur.getInt(4) == 1;
            list.add(c);

            cur.moveToNext();
        }

        cur.close();

        return list;
    }
	
	public static List<Expense> GetUnsyncedExpenses(String budgetId, String state)
	{
        String where = "BudgetId = ? AND State = ?";
        String[] whereArgs = new String[]{budgetId, state};

        return queryExpenses(where, whereArgs, null);
	}

    public static List<Category> GetUnsyncedCategories(String budgetId, String state)
    {
        String where = "BudgetId = ? AND State = ?";
        String[] whereArgs = new String[]{budgetId, state};

        return queryCategories(where, whereArgs, null);
    }

	public static List<Expense> GetExpensesForWeek(String budgetId, int daysBackFromToday, int startDay)
	{
		String[] dates = getStartAndEndDatesForWeek(daysBackFromToday, startDay);
        String startString = dates[0];
        String endString = dates[1];

    	String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ?";
    	String[] whereArgs = new String[]{budgetId, startString, endString, DELETEDSTATEKEY};
    	String orderBy = "Date asc";
		
		return queryExpenses(where, whereArgs, orderBy);
	}

    private static String[] getStartAndEndDatesForWeek(int daysBackFromToday, int startDay)
    {
        Calendar start=Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
        while((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay)
        {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 7);

        String startString = getDateString(start.getTime());
        String endString = getDateString(end.getTime());

        return new String[]{startString, endString};
    }

    private static String[] getStartAndEndDatesForMonth(int daysBackFromToday)
    {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
        while(start.get(Calendar.DAY_OF_MONTH) > 1)
        {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        String startString = getDateString(start.getTime());
        String endString = getDateString(end.getTime());

        return new String[]{startString, endString};
    }

    private static List<CategoryAmount> getCategoryAmounts(String budgetId, String startString, String endString, String uncategorizedString)
    {
        String query = "SELECT e.CategoryId, c.Name, SUM(e.Amount) FROM "+ EXPENSESTABLENAME +" e LEFT OUTER JOIN "+ CATEGORIESTABLENAME +" c ON c.Id = e.CategoryId WHERE e.BudgetId = ? AND e.Date >= ? AND e.Date < ? AND e.IsSystem = 0 GROUP BY e.CategoryId, c.Name HAVING SUM(e.Amount) > 0 ORDER BY SUM(e.Amount) DESC";

        String[] whereArgs = new String[]{budgetId, startString, endString};

        Cursor c = myDB.rawQuery(query, whereArgs);

        List<CategoryAmount> list = new ArrayList<CategoryAmount>();

        c.moveToFirst();

        while(!c.isAfterLast())
        {
            CategoryAmount ca = new CategoryAmount();
            ca.CategoryId = !c.isNull(0) ? c.getLong(0) : null;
            String name = c.getString(1);
            ca.Name = name != null ? name : uncategorizedString;
            ca.Amount = c.getDouble(2);
            list.add(ca);

            c.moveToNext();
        }

        c.close();

        return list;
    }

    public static List<CategoryAmount> GetCategoryAmountsForWeek(String budgetId, int daysBackFromToday, int startDay, String uncategorizedString)
    {
        String[] dates = getStartAndEndDatesForWeek(daysBackFromToday, startDay);
        String startString = dates[0];
        String endString = dates[1];

        return getCategoryAmounts(budgetId, startString, endString, uncategorizedString);
    }

    public static List<CategoryAmount> GetCategoryAmountsForMonth(String budgetId, int daysBackFromToday, String uncategorizedString)
    {
        String[] dates = getStartAndEndDatesForMonth(daysBackFromToday);
        String startString = dates[0];
        String endString = dates[1];

        return getCategoryAmounts(budgetId, startString, endString, uncategorizedString);
    }

    private static List<Expense> getExpensesForCategory(String budgetId, String categoryId, String startString, String endString)
    {
        String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ? AND IsSystem = 0";
        List<String> whereArgs = new ArrayList<String>();
        whereArgs.add(budgetId);
        whereArgs.add(startString);
        whereArgs.add(endString);
        whereArgs.add(DELETEDSTATEKEY);
        if(categoryId != null)
        {
            where += " AND CategoryId = ?";
            whereArgs.add(categoryId);
        }
        else
        {
            where += " AND CategoryId IS NULL";
        }

        String orderBy = "Date asc";

        return queryExpenses(where, whereArgs.toArray(new String[0]), orderBy);
    }

    public static List<Expense> GetExpensesForCategoryForWeek(String budgetId, String categoryId, int daysBackFromToday, int startDay)
    {
        String[] dates = getStartAndEndDatesForWeek(daysBackFromToday, startDay);
        String startString = dates[0];
        String endString = dates[1];

        return getExpensesForCategory(budgetId, categoryId, startString, endString);
    }

    public static List<Expense> GetExpensesForCategoryForMonth(String budgetId, String categoryId, int daysBackFromToday)
    {
        String[] dates = getStartAndEndDatesForMonth(daysBackFromToday);
        String startString = dates[0];
        String endString = dates[1];

        return getExpensesForCategory(budgetId, categoryId, startString, endString);
    }
	
	public static double GetTotalForMonth(String budgetId, int daysBackFromToday)
	{
        String[] dates = getStartAndEndDatesForMonth(daysBackFromToday);
        String startString = dates[0];
        String endString = dates[1];
    	
    	String[] columns = new String[]{"SUM(Amount)"};
    	String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ? AND IsSystem = 0";
    	String[] whereArgs = new String[]{budgetId, startString, endString, DELETEDSTATEKEY};
		
		Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs,null,null, null);
		c.moveToFirst();
		
		double total = c.getDouble(0);
		
		return total;
	}
	
	public static List<DateTotal> GetTotalsForMonth(String budgetId, int daysBackFromToday, int startDay)
	{
		Calendar start = Calendar.getInstance();
		start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
		int month = start.get(Calendar.MONTH);
		while(start.get(Calendar.DAY_OF_MONTH) > 1)
		{
			start.add(Calendar.DAY_OF_YEAR, -1);
		}
		while((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay)
		{
			start.add(Calendar.DAY_OF_YEAR, -1);
		}
		
		Calendar end = (Calendar) start.clone();
		end.add(Calendar.DAY_OF_YEAR, 7);
		boolean first = true;
		
		List<DateTotal> list = new ArrayList<DateTotal>();
		
		while(first || start.get(Calendar.MONTH) == month)
		{
			if(first)
				first = false;
			
			String startString = getDateString(start.getTime());
	    	String endString = getDateString(end.getTime());
	    	
	    	String[] columns = new String[]{"Amount"};
	    	String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ? AND IsSystem = 0";
	    	String[] whereArgs = new String[]{budgetId, startString, endString, DELETEDSTATEKEY};
	    	String orderBy = "Date asc";
	    	
	    	DateTotal dt = new DateTotal(start, 0);
			
			Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs,null,null, orderBy);
			c.moveToFirst();
			while(!c.isAfterLast())
			{
				dt.Total += c.getDouble(0);

				c.moveToNext();
			}
			c.close();
			
			list.add(dt);
			
			start.add(Calendar.DAY_OF_YEAR, 7);
			end.add(Calendar.DAY_OF_YEAR, 7);
		}

		return list;
	}

    public static boolean SystemExpenseExistsForWeek(String budgetId, int daysBackFromToday, int startDay)
    {
        String[] dates = getStartAndEndDatesForWeek(daysBackFromToday, startDay);
        String startString = dates[0];
        String endString = dates[1];

        String query = "SELECT 1 FROM " + EXPENSESTABLENAME + " WHERE BudgetId = ? AND IsSystem = 1 AND State != ? AND Date >= ? AND Date < ?";
        String[] args = new String[]{budgetId, DELETEDSTATEKEY, startString, endString};

        Cursor c = myDB.rawQuery(query, args);

        return c.getCount() >= 1;
    }

    private static String getDateString(Date date)
    {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return formatter.format(date);
    }
    private static Date getDate(String date)
    {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }
}