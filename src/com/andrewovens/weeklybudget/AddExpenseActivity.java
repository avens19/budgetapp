package com.andrewovens.weeklybudget;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import org.json.JSONException;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class AddExpenseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_expense);

		DatePicker dp = (DatePicker)findViewById(R.id.add_date);
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		
		dp.init(year, month, day, null);
	}
	
	@SuppressWarnings("deprecation")
	public void addButtonOnClick(View v)
	{
		final Expense e = new Expense();
		DatePicker dp = (DatePicker)findViewById(R.id.add_date);
		e.Date = new Date(dp.getYear() - 1900, dp.getMonth(), dp.getDayOfMonth());
		EditText description = (EditText)findViewById(R.id.add_description);
		e.Description = description.getText().toString();
		EditText amount = (EditText)findViewById(R.id.add_amount);
		e.Amount = Double.parseDouble(amount.getText().toString());
		e.BudgetId = Settings.getBudgetId(this);
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					API.AddExpense(e);
					AddExpenseActivity.this.finish();
				} catch (Exception e) {
					Settings.showToastOnUi(AddExpenseActivity.this, R.string.error_network, Toast.LENGTH_SHORT);
					e.printStackTrace();
				}
			}
			
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add_expense, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
