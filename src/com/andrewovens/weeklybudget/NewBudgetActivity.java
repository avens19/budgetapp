package com.andrewovens.weeklybudget;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class NewBudgetActivity extends Activity {
	
	private Budget _budget;
	private boolean _isEdit = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_budget);
		
		Intent i = getIntent();
		String budget = i.getStringExtra("budget");
		
		TextView uniqueId = (TextView)findViewById(R.id.text_new_unique);
		
		if(budget == null)
		{		
			_budget = new Budget(true);
			
			uniqueId.setText(_budget.UniqueId);
		}
		else
		{
			try
			{
				_isEdit = true;
				this.setTitle(R.string.edit_budget_title);
				
				_budget = Budget.fromJson(new JSONObject(budget));
				
				Spinner weekday = (Spinner)findViewById(R.id.weekday_spinner);
				EditText amount = (EditText)findViewById(R.id.text_new_amount);
				Button edit = (Button)findViewById(R.id.button_create_budget);
				
				weekday.setSelection(_budget.StartDay);
				amount.setText(_budget.Amount + "");
				uniqueId.setText(_budget.UniqueId);
				edit.setText(R.string.button_edit_budget);
			}
			catch(Exception e)
			{
				this.finish();
			}
		}
	}
	
	public void uniqueIdOnClick(View v)
	{
		ClipboardManager clipboard = (ClipboardManager)
		        getSystemService(Context.CLIPBOARD_SERVICE);
		
		clipboard.setPrimaryClip(ClipData.newPlainText("uniqueId", _budget.UniqueId));
		
		Toast.makeText(this, R.string.copied_unique_id, Toast.LENGTH_SHORT).show();
	}
	
	public void goButtonOnClick(View v)
	{
		Spinner weekday = (Spinner)findViewById(R.id.weekday_spinner);
		EditText amount = (EditText)findViewById(R.id.text_new_amount);
		
		_budget.StartDay = weekday.getSelectedItemPosition();
		_budget.Amount = Integer.parseInt(amount.getText().toString());
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Looper.prepare();
					
					if(_isEdit)
					{
						API.EditBudget(_budget);
						
						NewBudgetActivity.this.finish();
					}
					else
					{
						_budget = API.CreateBudget(_budget);
						
						Settings.setBudgetId(NewBudgetActivity.this, _budget.UniqueId);
						
						Intent i = new Intent(NewBudgetActivity.this, WeekActivity.class);
						startActivity(i);
						NewBudgetActivity.this.setResult(RESULT_OK);
						NewBudgetActivity.this.finish();
					}
					
				} catch (Exception e) {
					Settings.showToastOnUi(NewBudgetActivity.this, R.string.error_network, Toast.LENGTH_SHORT);
					e.printStackTrace();
				}
			}
			
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.new_budget, menu);
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
