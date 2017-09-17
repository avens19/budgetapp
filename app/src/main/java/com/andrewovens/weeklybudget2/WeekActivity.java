package com.andrewovens.weeklybudget2;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.view.MotionEvent;
import android.widget.Toast;

public class WeekActivity extends Activity implements ActionBar.OnNavigationListener {

	private Budget _budget;
    private BroadcastReceiver _syncReceiver;
	private int _daysBackFromToday = 0;

	private static final int EDIT_BUDGET = 1;
	private static final int SWITCH_BUDGET = 2;

	private static final int MONTH_ACTIVITY = 101;
	private static final int CATEGORY_WEEK_ACTIVITY = 102;
	private static final int CATEGORY_MONTH_ACTIVITY = 103;
    private static final int CATEGORY_ACTIVITY = 104;
	private static final int FIRST_ACTIVITY = 105;

	public static final String GOTO_ACTIVITY = "GOTO_ACTIVITY";

	public static final int GOTO_WEEK = 200;
	public static final int GOTO_MONTH = 201;
	public static final int GOTO_CATEGORY_WEEK = 202;
	public static final int GOTO_CATEGORY_MONTH = 203;
    public static final int GOTO_CATEGORY = 204;

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
						new String[]{
								getString(R.string.title_week),
								getString(R.string.title_month),
								getString(R.string.title_category_week),
								getString(R.string.title_category_month),
								getString(R.string.title_category),
						}),
				this);

		setUpSwipe();
        ListView lv = (ListView)WeekActivity.this.findViewById(R.id.week_list);
        registerForContextMenu(lv);

        IntentFilter syncFilter = new IntentFilter(SyncService.SYNCCOMPLETE);
        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final View spinner = findViewById(R.id.main_load);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (spinner != null) {
                            spinner.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        };
        registerReceiver(_syncReceiver, syncFilter);
	}

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(_syncReceiver);
        super.onDestroy();
    }

	@Override
	protected void onResume()
	{
		super.onResume();

		DBHelper.OpenDB(this);

		ActionBar actionBar = getActionBar();

		actionBar.setSelectedNavigationItem(0);

		loadData();

		startSync();

        this.invalidateOptionsMenu();
	}

    private void startSync(){
        View spinner = findViewById(R.id.main_load);
        spinner.setVisibility(View.VISIBLE);
        SyncService.startSync(this);
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

	@SuppressLint("SimpleDateFormat") private String getPeriod()
	{
		Calendar start = Calendar.getInstance();
		start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
		while((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay)
		{
			start.add(Calendar.DAY_OF_YEAR, -1);
		}
		Calendar end = (Calendar) start.clone();
		end.add(Calendar.DAY_OF_YEAR, 6);
		return Dates.getShortDateString(this, start.getTime()) + " - " + Dates.getShortDateString(this, end.getTime());
	}

	private void loadData()
	{
		_budget = Settings.getBudget(this);

		// Check for first run.
		if(_budget == null)
		{
			Intent k = new Intent(this, FirstActivity.class);
			startActivityForResult(k, FIRST_ACTIVITY);
			return;
		}

        Budget[] budgets = Settings.getBudgets(this);

		// Migration to add Budget name and support multiple budgets
		if(budgets == null)
		{
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						Looper.prepare();

						Budget budget = API.GetBudget(_budget.UniqueId);

						Settings.setBudget(WeekActivity.this, budget);

						Budget[] bs = new Budget[1];
						bs[0] = budget;

						Settings.setBudgets(WeekActivity.this, bs);

						_budget = Budget.update(_budget, budget);
					} catch (Exception e) {
						Helpers.showToastOnUi(WeekActivity.this, R.string.error_network, Toast.LENGTH_SHORT);
						e.printStackTrace();
					}
				}

			}).start();
		}

        // Check for open from widget
		if(this.getIntent().getBooleanExtra("ADD", false))
		{
			this.getIntent().putExtra("ADD", false);
			Intent i = new Intent(WeekActivity.this, AddExpenseActivity.class);
			startActivity(i);
			return;
		}

		List<Expense> expenses = DBHelper.GetExpensesForWeek(_budget.UniqueId, _daysBackFromToday, _budget.StartDay);

        double total = 0;
        for(int i = 0;i < expenses.size(); i++)
        {
            total += expenses.get(i).Amount;
        }
        double remaining = _budget.Amount - total;

        Button r = (Button)WeekActivity.this.findViewById(R.id.remaining);
        final double rounded = Math.round(remaining*100)/100.0;
        if(rounded >= 0)
        {
            r.setText("Remaining: " + Helpers.currencyString(rounded));
            r.setTextColor(Color.BLACK);
        }
        else
        {
            r.setText("Over: " + Helpers.currencyString(Math.abs(rounded)));
            r.setTextColor(Color.RED);
        }
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(DBHelper.SystemExpenseExistsForWeek(_budget.UniqueId, _daysBackFromToday - 7, _budget.StartDay))
                {
                    Toast.makeText(WeekActivity.this, R.string.already_carried, Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(WeekActivity.this);

                builder
                        .setTitle(R.string.carry_balance)
                        .setMessage(R.string.carry_balance_message)
                        .setCancelable(true)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Calendar start=Calendar.getInstance();
                                start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
                                while((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay)
                                {
                                    start.add(Calendar.DAY_OF_YEAR, -1);
                                }
                                start.add(Calendar.DAY_OF_YEAR, 7);
                                Expense e = new Expense();
                                e.Amount = -rounded;
                                e.Date = new GregorianCalendar(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).getTime();
                                e.BudgetId = _budget.UniqueId;
                                e.Id = Settings.getNextId(WeekActivity.this);
                                e.Description = getString(R.string.carry_balance_expense_description);
                                e.IsSystem = true;
                                DBHelper.AddExpense(e, DBHelper.CREATEDSTATEKEY);
                                loadData();
                                dialog.dismiss();
                            }
                        });

                Dialog d = builder.create();

                d.show();
            }
        });
        ListView lv = (ListView)WeekActivity.this.findViewById(R.id.week_list);
        if (lv.getAdapter() == null) {
            WeekRowAdapter adapter = new WeekRowAdapter(WeekActivity.this, R.layout.week_row, expenses, DayType.DayOfWeek);
            lv.setAdapter(adapter);
        } else {
            ((WeekRowAdapter)lv.getAdapter()).clear();
            ((WeekRowAdapter)lv.getAdapter()).addAll(expenses);
        }

        TextView dates = (TextView)findViewById(R.id.current_week);
        dates.setText(getPeriod());

        Intent intent = new Intent(WeekActivity.this, AddExpenseWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AddExpenseWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
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
		_daysBackFromToday -= 7;
		loadData();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ListView lv = (ListView)v;
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		lv.setTag(lv.getAdapter().getItem(info.position));
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.week_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
        ListView lv = (ListView)findViewById(R.id.week_list);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		Expense e = (Expense)lv.getItemAtPosition(info.position);

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
			case R.id.context_copy:
				copyExpense(e);
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
		if(e.State.equals(DBHelper.CREATEDSTATEKEY))
			DBHelper.DeleteExpense(e);
		else
			DBHelper.EditExpense(e, DBHelper.DELETEDSTATEKEY);
		loadData();
        SyncService.startSync(this);
	}

	private void copyExpense(Expense e)
	{
		Calendar start=Calendar.getInstance();
		start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
		while((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay)
		{
			start.add(Calendar.DAY_OF_YEAR, -1);
		}
		start.add(Calendar.DAY_OF_YEAR, 7);
		Expense ex = new Expense();
		ex.Amount = e.Amount;
		ex.Date = new GregorianCalendar(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).getTime();
		ex.BudgetId = _budget.UniqueId;
		ex.Id = Settings.getNextId(WeekActivity.this);
		ex.Description = e.Description;
		ex.CategoryId = e.CategoryId;
		DBHelper.AddExpense(ex, DBHelper.CREATEDSTATEKEY);
		loadData();
		SyncService.startSync(this);
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
        if(_budget != null) {
            MenuItem s = menu.findItem(R.id.action_current_budget);
            s.setTitle(this.getString(R.string.current_budget) + " " + _budget.Name);
        }
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if(id == R.id.action_current_budget)
		{
			if(_budget != null)
			{
				try
				{
					Intent i = new Intent(this, SwitchBudgetActivity.class);
					startActivityForResult(i, SWITCH_BUDGET);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			return true;
		}
		if (id == R.id.action_settings) {
			if(_budget != null)
			{
				try
				{
					Intent i = new Intent(this, NewBudgetActivity.class);
					i.putExtra("budget", _budget.toJson(false).toString());
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
			gotoMonth();
		}
		else if(position == 2 && _budget != null)
		{
			gotoCategoryWeek();
		}
		else if(position == 3 && _budget != null)
		{
			gotoCategoryMonth();
		}
        else if(position == 4 && _budget != null)
        {
            gotoCategory();
        }
		return true;
	}

	private void gotoMonth()
	{
		try
		{
			Intent i = new Intent(this, MonthActivity.class);
			i.putExtra("budget", _budget.toJson(false).toString());
			i.putExtra("days", _daysBackFromToday);
			startActivityForResult(i, MONTH_ACTIVITY);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void gotoCategoryWeek()
	{
		try
		{
			Intent i = new Intent(this, CategoryWeekActivity.class);
			i.putExtra("budget", _budget.toJson(false).toString());
			i.putExtra("days", _daysBackFromToday);
			startActivityForResult(i, CATEGORY_WEEK_ACTIVITY);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void gotoCategoryMonth()
	{
		try
		{
			Intent i = new Intent(this, CategoryMonthActivity.class);
			i.putExtra("budget", _budget.toJson(false).toString());
			i.putExtra("days", _daysBackFromToday);
			startActivityForResult(i, CATEGORY_MONTH_ACTIVITY);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

    private void gotoCategory()
    {
        try
        {
            Intent i = new Intent(this, CategoryActivity.class);
            i.putExtra("budget", _budget.toJson(false).toString());
            i.putExtra("days", _daysBackFromToday);
            startActivityForResult(i, CATEGORY_ACTIVITY);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void gotoActivity(int go)
    {
        switch(go)
        {
            case GOTO_MONTH:
                gotoMonth();
                break;
            case GOTO_CATEGORY_WEEK:
                gotoCategoryWeek();
                break;
            case GOTO_CATEGORY_MONTH:
                gotoCategoryMonth();
                break;
            case GOTO_CATEGORY:
                gotoCategory();
                break;
        }
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if (requestCode == FIRST_ACTIVITY && resultCode != Activity.RESULT_OK) {
			this.finish();
			return;
		}

        int days = -1;
        int activity = GOTO_WEEK;
        if (data != null)
        {
            days = data.getIntExtra("days", -1);
            activity = data.getIntExtra(GOTO_ACTIVITY, GOTO_WEEK);
        }
        if (days != -1) {
            _daysBackFromToday = data.getIntExtra("days", 0);
        }

        gotoActivity(activity);
	}
}
