package com.andrewovens.weeklybudget;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class AddExpenseActivity extends Activity {
	
	private boolean _isEdit = false;
	private Expense _expense;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_expense);
		
		Intent i = this.getIntent();
		String expenseString = i.getStringExtra("expense");
		DatePicker dp = (DatePicker)findViewById(R.id.add_date);
		
		if(expenseString != null)
		{
			try
			{
				_isEdit = true;
				_expense = Expense.fromJson(new JSONObject(expenseString));
				Calendar c = Calendar.getInstance();
				c.setTime(_expense.Date);
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH);
				int day = c.get(Calendar.DAY_OF_MONTH);
				dp.init(year, month, day, null);
				this.setTitle(R.string.edit_title);
				EditText description = (EditText)findViewById(R.id.add_description);
				description.setText(_expense.Description);
				EditText amount = (EditText)findViewById(R.id.add_amount);
				amount.setText(_expense.Amount + "");
				Button edit = (Button)findViewById(R.id.add_add_button);
				edit.setText(R.string.edit_button);
			}
			catch(Exception e)
			{
				this.finish();
			}
		}
		else
		{
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			
			dp.init(year, month, day, null);
		}
	}
	
	@SuppressWarnings("deprecation")
	public void addButtonOnClick(View v)
	{
		final Expense e = new Expense();
		if(_isEdit)
			e.Id = _expense.Id;
		
		DatePicker dp = (DatePicker)findViewById(R.id.add_date);
		e.Date = new GregorianCalendar(dp.getYear(), dp.getMonth(), dp.getDayOfMonth()).getTime();
		EditText description = (EditText)findViewById(R.id.add_description);
		e.Description = description.getText().toString();
		EditText amount = (EditText)findViewById(R.id.add_amount);
		e.Amount = Double.parseDouble(amount.getText().toString());
		e.BudgetId = Settings.getBudgetId(this);
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					if(!_isEdit)
						API.AddExpense(e);
					else
						API.EditExpense(e);
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
