package com.andrewovens.weeklybudget;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class NewBudgetActivity extends Activity {
	
	private Budget _budget;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_budget);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		_budget = new Budget(true);
		
		EditText uniqueId = (EditText)findViewById(R.id.text_new_unique);
		uniqueId.setText(_budget.UniqueId);
	}
	
	public void goButtonOnClick(View v)
	{
		Spinner weekday = (Spinner) findViewById(R.id.weekday_spinner);
		EditText amount = (EditText)findViewById(R.id.text_new_amount);
		
		_budget.StartDay = weekday.getSelectedItemPosition();
		_budget.Amount = Integer.parseInt(amount.getText().toString());
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					API.CreateBudget(_budget);
					
					Intent i = new Intent(NewBudgetActivity.this, WeekActivity.class);
					startActivity(i);
					NewBudgetActivity.this.finish();
					
				} catch (Exception e) {
					Toast.makeText(NewBudgetActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
			
		});
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_new_budget,
					container, false);
			return rootView;
		}
	}

}
