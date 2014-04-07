package com.andrewovens.weeklybudget;

import org.json.JSONObject;

import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.os.Build;

public class MonthActivity extends Activity implements OnNavigationListener {

	private Budget _budget;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_month);

		// Set up the action bar to show a dropdown list.
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(
						actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1,
						new String[] {
							getString(R.string.title_week),
							getString(R.string.title_month),
						}),
						this);

		try
		{
			String budgetString = getIntent().getStringExtra("budget");
			_budget = Budget.fromJson(new JSONObject(budgetString));
		}
		catch(Exception e)
		{
			this.finish();
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		ActionBar actionBar = getActionBar();
		actionBar.setSelectedNavigationItem(1);
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		if(position == 0)
		{
			try
			{
				this.finish();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.month, menu);
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
