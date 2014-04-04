package com.andrewovens.weeklybudget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class Settings {
	public static final String SETTINGS_NAME = "WEEKLY_BUDGET_SETTINGS";
	public static final String BUDGET_ID = "BUDGET_ID";
	public static final String AMOUNT = "AMOUNT";
	
	public static String getBudgetId(Context cxt)
	{
		SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
	    return settings.getString(BUDGET_ID, null);
	}
	public static void setBudgetId(Context cxt, String id)
	{
		SharedPreferences settings = cxt.getSharedPreferences(SETTINGS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
	    editor.putString(BUDGET_ID, id);
	    editor.commit();
	}
	public static void showToastOnUi(final Activity a, final int resId, final int length)
	{
		a.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				Toast.makeText(a, resId, length).show();
			}
			
		});
	}
}
