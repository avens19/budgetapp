package com.andrewovens.weeklybudget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.MotionEvent;

public class WeekActivity extends Activity implements ActionBar.OnNavigationListener {

	private Budget _budget;
	private WeekRowAdapter _adapter;
	private int _daysBackFromToday = 0;

	private final int EDIT_BUDGET = 1;

	private int MONTH_ACTIVITY = 101;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_week);

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

		setUpSwipe();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		DBHelper.OpenDB(this);
		DBHelper.CreateExpensesTable();

		ActionBar actionBar = getActionBar();

		actionBar.setSelectedNavigationItem(0);

		loadData();
	}

	private void setUpSwipe()
	{
		final View container = findViewById(R.id.week_container);
		View v = findViewById(R.id.week_list);

		container.setOnTouchListener(new OnSwipeTouchListener(this) {
			public void onSwipeRight() {
				weekBack();
			}
			public void onSwipeLeft() {
				weekForward();
			}

			public boolean onTouch(View v, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return true;
			}
		});
		v.setOnTouchListener(new OnSwipeTouchListener(this) {
			public void onSwipeRight() {
				weekBack();
			}
			public void onSwipeLeft() {
				weekForward();
			}

			public boolean onTouch(View v, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return false;
			}
		});
	}

	private String getPeriod()
	{
		Calendar start = Calendar.getInstance();
		start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
		while((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay)
		{
			start.add(Calendar.DAY_OF_YEAR, -1);
		}
		Calendar end = (Calendar) start.clone();
		end.add(Calendar.DAY_OF_YEAR, 6);
		DateFormat df = new SimpleDateFormat("MM/dd");
		return df.format(start.getTime()) + " - " + df.format(end.getTime());
	}

	private void loadData()
	{
		_budget = Settings.getBudget(this);

		// Check for first run.
		if(_budget == null)
		{
			Intent k = new Intent(this, FirstActivity.class);
			startActivity(k);
			this.finish();
			return;
		}

		setUpOnLongClick();

		ListView lv = (ListView)WeekActivity.this.findViewById(R.id.week_list);
		registerForContextMenu(lv);

		new Thread(new Runnable(){

			@Override
			public void run() {
				try
				{
					if(Settings.getWatermark(WeekActivity.this) == null)
					{
						_budget = Settings.getBudget(WeekActivity.this);
						String d = Dates.UTCTimeString();
						List<Expense> expenses = API.GetExpenses(_budget.UniqueId);
						Settings.setWatermark(WeekActivity.this, d);

						for(Expense e : expenses)
						{
							if(!e.IsDeleted)
								DBHelper.AddExpense(e, "synced");
						}
					}

					List<Expense> expenses = DBHelper.GetExpensesForWeek(_daysBackFromToday, _budget.StartDay);

					double total = 0;
					for(int i = 0;i < expenses.size(); i++)
					{
						total += expenses.get(i).Amount;
					}
					double remaining = _budget.Amount - total;

					UpdateView(expenses, remaining);

					SyncData();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}

		}).start();
	}

	public void UpdateView(final List<Expense> expenses, final double remaining)
	{
		Runnable update = new Runnable(){
			@Override
			public void run() {
				synchronized(this)
				{
					TextView r = (TextView)WeekActivity.this.findViewById(R.id.remaining);
					double rounded = Math.round(remaining*100)/100.0;
					if(rounded >= 0)
					{
						r.setText("Remaining: $" + Helpers.doubleString(rounded));
						r.setTextColor(Color.BLACK);
					}
					else
					{
						r.setText("Over: $" + Helpers.doubleString(Math.abs(rounded)));
						r.setTextColor(Color.RED);
					}
					ListView lv = (ListView)WeekActivity.this.findViewById(R.id.week_list);
					_adapter = new WeekRowAdapter(WeekActivity.this, R.layout.week_row, expenses);
					lv.setAdapter(_adapter);

					TextView dates = (TextView)findViewById(R.id.current_week);
					dates.setText(getPeriod());

					this.notify();
				}
			}
		};
		synchronized(update)
		{
			runOnUiThread(update);
			try {
				update.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void SyncData() throws Exception
	{
		_budget = API.GetBudget(_budget.UniqueId);
		
		Settings.setBudget(this, _budget);
		
		List<Expense> expenses = DBHelper.GetUnsyncedExpenses("created");
		for(Expense e : expenses)
		{
			Expense expense = API.AddExpense(e);
			DBHelper.DeleteExpense(e);
			DBHelper.AddExpense(expense, "synced");
		}

		expenses = DBHelper.GetUnsyncedExpenses("edited");
		for(Expense e : expenses)
		{
			API.EditExpense(e);
			DBHelper.EditExpense(e, "synced");
		}

		expenses = DBHelper.GetUnsyncedExpenses("deleted");
		for(Expense e : expenses)
		{
			API.DeleteExpense(e);
			DBHelper.DeleteExpense(e);
		}

		String d = Dates.UTCTimeString();
		List<Expense> newExpenses = API.GetExpenses(_budget.UniqueId, Settings.getWatermark(WeekActivity.this));
		Settings.setWatermark(WeekActivity.this, d);

		for(Expense e : newExpenses)
		{
			if(!e.IsDeleted)
				DBHelper.AddExpense(e, "synced");
			else
				DBHelper.DeleteExpense(e);
		}

		expenses = DBHelper.GetExpensesForWeek(_daysBackFromToday, _budget.StartDay);
		double total = 0;
		for(int i = 0;i < expenses.size(); i++)
		{
			total += expenses.get(i).Amount;
		}
		double remaining = _budget.Amount - total;

		UpdateView(expenses, remaining);
	}

	public void weekBackOnClick(View v)
	{
		weekBack();
	}
	
	private void weekBack()
	{
		_daysBackFromToday += 7;
		loadData();
	}

	public void weekForwardOnClick(View v)
	{
		weekForward();
	}
	
	private void weekForward()
	{
		if(_daysBackFromToday > 0)
		{
			_daysBackFromToday -= 7;
			loadData();
		}
	}

	private void setUpOnLongClick()
	{
		ListView lv = (ListView)WeekActivity.this.findViewById(R.id.week_list);
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				parent.setTag(parent.getItemAtPosition(position));
				WeekActivity.this.openContextMenu(parent);
				return false;
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.week_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		Expense e = _adapter.get(info.position);

		try
		{
			switch(item.getItemId())
			{
			case R.id.context_edit:
				Intent i = new Intent(WeekActivity.this, AddExpenseActivity.class);
				i.putExtra("expense", e.toJson().toString());
				WeekActivity.this.startActivity(i);
				break;
			case R.id.context_delete:
				deleteExpense(e);
				break;
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}

		return true;
	}

	private void deleteExpense(Expense e)
	{
		if(e.State.equals("created"))
			DBHelper.DeleteExpense(e);
		else
			DBHelper.EditExpense(e, "deleted");
		loadData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.week, menu);
		MenuItem add = menu.findItem(R.id.action_add);
		add.setOnMenuItemClickListener(new OnMenuItemClickListener(){

			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				Intent i = new Intent(WeekActivity.this, AddExpenseActivity.class);
				startActivity(i);
				return true;
			}});
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			if(_budget != null)
			{
				try
				{
					Intent i = new Intent(this, NewBudgetActivity.class);
					i.putExtra("budget", _budget.toJson().toString());
					i.putExtra("days", _daysBackFromToday);
					startActivityForResult(i, EDIT_BUDGET);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		if(position == 1 && _budget != null)
		{
			try
			{
				Intent i = new Intent(this, MonthActivity.class);
				i.putExtra("budget", _budget.toJson().toString());
				i.putExtra("days", _daysBackFromToday);
				startActivityForResult(i, MONTH_ACTIVITY);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if(requestCode == MONTH_ACTIVITY && resultCode == Activity.RESULT_OK)
		{
			_daysBackFromToday = data.getIntExtra("days", 0);
			loadData();
		}
	}

	public class WeekRowAdapter extends ArrayAdapter<Expense> {
		private final Context context;
		private final int resourceID;
		private List<Expense> list;

		public WeekRowAdapter(Context context, int resource, List<Expense> bah) {
			super(context, resource, bah);

			this.context = context;
			this.resourceID = resource;
			this.list = bah;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(resourceID, parent, false);

			TextView day = (TextView)rowView.findViewById(R.id.week_row_day);
			day.setText(Dates.getWeekDay(list.get(position).Date));

			TextView name = (TextView)rowView.findViewById(R.id.week_row_name);
			name.setText(list.get(position).Description);

			TextView amount = (TextView)rowView.findViewById(R.id.week_row_amount);
			amount.setText("$" + Helpers.doubleString(list.get(position).Amount));

			return rowView;
		}

		public Expense get(int position)
		{
			return list.get(position);
		}

	}
}
