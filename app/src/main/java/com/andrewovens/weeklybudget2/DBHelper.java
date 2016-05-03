package com.andrewovens.weeklybudget2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBHelper{

	public static final String DBNAME = "BUDGETDB";
	public static final String EXPENSESTABLENAME = "Expenses";
	public static final String CATEGORIESTABLENAME = "Categories";
	private static SQLiteDatabase myDB;
	
	/* Expense State column
	 * 
	 * created - created but not synced
	 * edited - edited but not synced
	 * deleted - deleted but not synced
	 * synced - synced to DB
	 */

	public static void OpenDB(Context c)
	{
		if(myDB == null || !myDB.isOpen())
			myDB = c.openOrCreateDatabase(DBNAME, SQLiteDatabase.OPEN_READWRITE, null);

		CreateExpensesTable();

		try
		{
			if(myDB.needUpgrade(1))
			{
				CreateCategoriesTable();
				AddCategoryFK();
				myDB.setVersion(1);
			}
            if(myDB.needUpgrade(2))
            {
                AddIsSystem();
                myDB.setVersion(2);
            }
		}
		catch(Exception e)
		{
			myDB.setVersion(0);
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
		ContentValues cv = new ContentValues();

		cv.put("Id", e.Id);
		cv.put("Date", Dates.getDateTimeString(e.Date));
		cv.put("Description", e.Description);
		cv.put("Amount", e.Amount);
		cv.put("BudgetId", e.BudgetId);
        cv.put("CategoryId", e.CategoryId);
		cv.put("State", state);
        cv.put("IsSystem", e.IsSystem ? 1 : 0);

		myDB.insertWithOnConflict(EXPENSESTABLENAME, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
	}

    public static void AddCategory(Category c, String state)
    {
        ContentValues cv = new ContentValues();

        cv.put("Id", c.Id);
        cv.put("Name", c.Name);
        cv.put("BudgetId", c.BudgetId);
        cv.put("State", state);
        cv.put("IsDeleted", c.IsDeleted ? 1 : 0);

        myDB.insertWithOnConflict(CATEGORIESTABLENAME, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
    }
	
	public static void EditExpense(Expense e, String state)
	{
		ContentValues cv = new ContentValues();

		cv.put("Id", e.Id);
		cv.put("Date", Dates.getDateTimeString(e.Date));
		cv.put("Description", e.Description);
		cv.put("Amount", e.Amount);
		cv.put("BudgetId", e.BudgetId);
        cv.put("CategoryId", e.CategoryId);
		cv.put("State", state);
        cv.put("IsSystem", e.IsSystem ? 1 : 0);
		
		String where = "Id = ?";
		String[] whereArgs = new String[]{Long.toString(e.Id)};

		myDB.update(EXPENSESTABLENAME, cv, where, whereArgs);
	}

    public static void EditCategory(Category c, String state)
    {
        ContentValues cv = new ContentValues();

        cv.put("Id", c.Id);
        cv.put("Name", c.Name);
        cv.put("BudgetId", c.BudgetId);
        cv.put("State", state);
        cv.put("IsDeleted", c.IsDeleted ? 1 : 0);

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
		String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "CategoryId", "State", "IsSystem"};
    	String where = "Id = ?";
    	String[] whereArgs = new String[]{Long.toString(id)};
		
		Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs,null,null, null);
		
		if(c.getCount() == 0)
			return null;

		c.moveToFirst();

		Expense e = new Expense();
		e.Id = c.getLong(0);
		e.Date = Dates.getDate(c.getString(1));
		e.Description = c.getString(2);
		e.Amount = c.getDouble(3);
		e.BudgetId = c.getString(4);
        e.CategoryId = !c.isNull(5) ? c.getLong(5) : null;
		e.State = c.getString(6);
        e.IsSystem = c.getInt(7) == 1;

		c.close();

		return e;
	}

    public static List<Category> GetActiveCategories(String budgetId, Long categoryId)
    {
        String[] columns = new String[]{"Id", "Name", "BudgetId", "State", "IsDeleted"};
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
		String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "CategoryId", "State", "IsSystem"};
    	String where = "BudgetId = ? AND State = ?";
    	String[] whereArgs = new String[]{budgetId, state};
		
		Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs,null,null, null);

		List<Expense> list = new ArrayList<Expense>();

		c.moveToFirst();

		while(!c.isAfterLast())
		{
			Expense e = new Expense();
			e.Id = c.getLong(0);
			e.Date = Dates.getDate(c.getString(1));
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

    public static List<Category> GetUnsyncedCategories(String budgetId, String state)
    {
        String[] columns = new String[]{"Id", "Name", "BudgetId", "State", "IsDeleted"};
        String where = "BudgetId = ? AND State = ?";
        String[] whereArgs = new String[]{budgetId, state};

        Cursor cur = myDB.query(CATEGORIESTABLENAME, columns, where, whereArgs,null,null, null);

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

	public static List<Expense> GetExpensesForWeek(String budgetId, int daysBackFromToday, int startDay)
	{
		Calendar start=Calendar.getInstance();
		start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
		while((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay)
    	{
    		start.add(Calendar.DAY_OF_YEAR, -1);
    	}
    	Calendar end = (Calendar) start.clone();
    	end.add(Calendar.DAY_OF_YEAR, 7);
    	
    	String startString = Dates.getDateString(start.getTime());
    	String endString = Dates.getDateString(end.getTime());
    	
    	String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "CategoryId", "State", "IsSystem"};
    	String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ?";
    	String[] whereArgs = new String[]{budgetId, startString, endString, "deleted"};
    	String orderBy = "Date asc";
		
		Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs,null,null, orderBy);

		List<Expense> list = new ArrayList<Expense>();

		c.moveToFirst();

		while(!c.isAfterLast())
		{
			Expense e = new Expense();
			e.Id = c.getLong(0);
			e.Date = Dates.getDate(c.getString(1));
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

    public static List<CategoryAmount> GetCategoryAmountsForWeek(String budgetId, int daysBackFromToday, int startDay, String uncategorizedString)
    {
        Calendar start=Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
        while((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay)
        {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 7);

        String startString = Dates.getDateString(start.getTime());
        String endString = Dates.getDateString(end.getTime());

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

    public static List<CategoryAmount> GetCategoryAmountsForMonth(String budgetId, int daysBackFromToday, String uncategorizedString)
    {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
        while(start.get(Calendar.DAY_OF_MONTH) > 1)
        {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        String startString = Dates.getDateString(start.getTime());
        String endString = Dates.getDateString(end.getTime());

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

    public static List<Expense> GetExpensesForCategoryForWeek(String budgetId, String categoryId, int daysBackFromToday, int startDay)
    {
        Calendar start=Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
        while((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay)
        {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 7);

        String startString = Dates.getDateString(start.getTime());
        String endString = Dates.getDateString(end.getTime());

        String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "CategoryId", "State"};
        String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ? AND IsSystem = 0";
        List<String> whereArgs = new ArrayList<String>();
        whereArgs.add(budgetId);
        whereArgs.add(startString);
        whereArgs.add(endString);
        whereArgs.add("deleted");
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

        Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs.toArray(new String[0]),null,null, orderBy);

        List<Expense> list = new ArrayList<Expense>();

        c.moveToFirst();

        while(!c.isAfterLast())
        {
            Expense e = new Expense();
            e.Id = c.getLong(0);
            e.Date = Dates.getDate(c.getString(1));
            e.Description = c.getString(2);
            e.Amount = c.getDouble(3);
            e.BudgetId = c.getString(4);
            e.CategoryId = !c.isNull(5) ? c.getLong(5) : null;
            e.State = c.getString(6);
            list.add(e);

            c.moveToNext();
        }

        c.close();

        return list;
    }

    public static List<Expense> GetExpensesForCategoryForMonth(String budgetId, String categoryId, int daysBackFromToday)
    {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
        while(start.get(Calendar.DAY_OF_MONTH) > 1)
        {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        String startString = Dates.getDateString(start.getTime());
        String endString = Dates.getDateString(end.getTime());

        String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "CategoryId", "State"};
        String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ? AND IsSystem = 0";
        List<String> whereArgs = new ArrayList<String>();
        whereArgs.add(budgetId);
        whereArgs.add(startString);
        whereArgs.add(endString);
        whereArgs.add("deleted");
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

        Cursor c = myDB.query(EXPENSESTABLENAME, columns, where, whereArgs.toArray(new String[0]),null,null, orderBy);

        List<Expense> list = new ArrayList<Expense>();

        c.moveToFirst();

        while(!c.isAfterLast())
        {
            Expense e = new Expense();
            e.Id = c.getLong(0);
            e.Date = Dates.getDate(c.getString(1));
            e.Description = c.getString(2);
            e.Amount = c.getDouble(3);
            e.BudgetId = c.getString(4);
            e.CategoryId = !c.isNull(5) ? c.getLong(5) : null;
            e.State = c.getString(6);
            list.add(e);

            c.moveToNext();
        }

        c.close();

        return list;
    }
	
	public static double GetTotalForMonth(String budgetId, int daysBackFromToday)
	{
		Calendar start = Calendar.getInstance();
		start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
		while(start.get(Calendar.DAY_OF_MONTH) > 1)
		{
			start.add(Calendar.DAY_OF_YEAR, -1);
		}
		
		Calendar end = (Calendar) start.clone();
		end.add(Calendar.MONTH, 1);
		
		String startString = Dates.getDateString(start.getTime());
    	String endString = Dates.getDateString(end.getTime());
    	
    	String[] columns = new String[]{"SUM(Amount)"};
    	String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ? AND IsSystem = 0";
    	String[] whereArgs = new String[]{budgetId, startString, endString, "deleted"};
		
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
			
			String startString = Dates.getDateString(start.getTime());
	    	String endString = Dates.getDateString(end.getTime());
	    	
	    	String[] columns = new String[]{"Amount"};
	    	String where = "BudgetId = ? AND Date >= ? AND Date < ? AND State != ? AND IsSystem = 0";
	    	String[] whereArgs = new String[]{budgetId, startString, endString, "deleted"};
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
        Calendar start=Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, daysBackFromToday * -1);
        while((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay)
        {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 7);

        String startString = Dates.getDateString(start.getTime());
        String endString = Dates.getDateString(end.getTime());

        String query = "SELECT 1 FROM " + EXPENSESTABLENAME + " WHERE BudgetId = ? AND IsSystem = 1 AND State != ? AND Date >= ? AND Date < ?";
        String[] args = new String[]{budgetId, "deleted", startString, endString};

        Cursor c = myDB.rawQuery(query, args);

        return c.getCount() >= 1;
    }
}