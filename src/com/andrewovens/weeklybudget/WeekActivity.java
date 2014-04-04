package com.andrewovens.weeklybudget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WeekActivity extends Activity implements ActionBar.OnNavigationListener {

    private Budget _budget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
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
        
        final String budgetId = Settings.getBudgetId(this);
        
        // Check for first run.
        if(budgetId == null)
        {
	        Intent k = new Intent(this, FirstActivity.class);
	        startActivity(k);
	        this.finish();
	        return;
        }
        
        new Thread(new Runnable(){

			@Override
			public void run() {
				try
				{
					_budget = API.GetBudget(budgetId);
					List<Expense> expenses = API.GetWeek(budgetId);
					final List<String> expenseStrings = new ArrayList<String>();
					
					double total = 0;
					for(int i = 0;i < expenses.size(); i++)
					{
						Expense e = expenses.get(i);
						total += e.Amount;
						expenseStrings.add(API.getWeekDay(e.Date) + ": " + e.Amount);
					}
					final double remaining = _budget.Amount - total;
					
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							TextView r = (TextView)WeekActivity.this.findViewById(R.id.remaining);
							r.setText(Double.toString(remaining));
							ListView lv = (ListView)WeekActivity.this.findViewById(R.id.week_list);
							WeekRowAdapter ca = new WeekRowAdapter(WeekActivity.this, R.layout.week_row, expenseStrings);
							lv.setAdapter(ca);
						}
					});
				}
				catch(Exception e)
				{
					Settings.showToastOnUi(WeekActivity.this, R.string.error_network, Toast.LENGTH_SHORT);
					e.printStackTrace();
				}
			}
        	
        }).start();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.

        return true;
    }
    
    public class WeekRowAdapter extends ArrayAdapter<String> {
    	private final Context context;
    	private final int resourceID;
    	private List<String> list;

    	public WeekRowAdapter(Context context, int resource, List<String> bah) {
    	    super(context, resource, bah);

    	    this.context = context;
    	    this.resourceID = resource;
    	    this.list = bah;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    	    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	    View rowView = inflater.inflate(resourceID, parent, false);
    	    
    	    TextView tv = (TextView)rowView.findViewById(R.id.text_week_row);
    	    tv.setText(list.get(position));

    	    return rowView;

    	}

    }
}
