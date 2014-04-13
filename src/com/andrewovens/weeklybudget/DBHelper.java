package com.andrewovens.weeklybudget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper{

	public static final String DBNAME = "BUDGETDB";
	public static final String EXPENSESTABLENAME = "Expenses";
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

	public static void AddExpense(Expense e, String state)
	{
		ContentValues cv = new ContentValues();

		cv.put("Id", e.Id);
		cv.put("Date", Dates.getDateTimeString(e.Date));
		cv.put("Description", e.Description);
		cv.put("Amount", e.Amount);
		cv.put("BudgetId", e.BudgetId);
		cv.put("State", state);

		myDB.insertWithOnConflict(EXPENSESTABLENAME, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	public static void EditExpense(Expense e, String state)
	{
		ContentValues cv = new ContentValues();

		cv.put("Id", e.Id);
		cv.put("Date", Dates.getDateTimeString(e.Date));
		cv.put("Description", e.Description);
		cv.put("Amount", e.Amount);
		cv.put("BudgetId", e.BudgetId);
		cv.put("State", state);
		
		String where = "Id = ?";
		String[] whereArgs = new String[]{Long.toString(e.Id)};

		myDB.update(EXPENSESTABLENAME, cv, where, whereArgs);
	}
	
	public static void DeleteExpense(Expense e)
	{
		String where = "Id = ?";
		String[] whereArgs = new String[]{Long.toString(e.Id)};
		
		myDB.delete(EXPENSESTABLENAME, where, whereArgs);		
	}
	
	public static Expense GetExpense(long id)
	{
		String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "State"};
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
		e.State = c.getString(5);

		c.close();

		return e;
	}
	
	public static List<Expense> GetUnsyncedExpenses(String state)
	{
		String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "State"};
    	String where = "State = ?";
    	String[] whereArgs = new String[]{state};
		
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
			e.State = c.getString(5);
			list.add(e);

			c.moveToNext();
		}

		c.close();

		return list;
	}

	public static List<Expense> GetExpensesForWeek(int daysBackFromToday, int startDay)
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
    	
    	String[] columns = new String[]{"Id", "Date", "Description", "Amount", "BudgetId", "State"};
    	String where = "Date >= ? AND Date < ? AND State != ?";
    	String[] whereArgs = new String[]{startString, endString, "deleted"};
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
			e.State = c.getString(5);
			list.add(e);

			c.moveToNext();
		}

		c.close();

		return list;
	}
	
	public static List<DateTotal> GetTotalsForMonth(int daysBackFromToday, int startDay)
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
	    	String where = "Date >= ? AND Date < ? AND State != ?";
	    	String[] whereArgs = new String[]{startString, endString, "deleted"};
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
}