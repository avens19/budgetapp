package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CategoryWeekActivity extends Activity implements ActionBar.OnNavigationListener, ValueFormatter, OnChartValueSelectedListener {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;
    private int _daysBackFromToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_week);

        // Set up the action bar to show a dropdown list.
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<>(
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
        setUpOnLongClick();

        try {
            Intent i = getIntent();
            String budgetString = i.getStringExtra("budget");
            _budget = Budget.fromJson(new JSONObject(budgetString));
            _daysBackFromToday = i.getIntExtra("days", 0);
        } catch (Exception e) {
            this.finish();
            e.printStackTrace();
        }

        PieChart chart = findViewById(R.id.week_chart);
        chart.setRotationEnabled(false);
        chart.setDescription("");
        chart.setHoleRadius(20f);
        chart.setTransparentCircleRadius(25f);

        chart.setOnChartValueSelectedListener(this);

        NonScrollableListView lv = findViewById(R.id.category_week_expense_list);
        lv.setFocusable(false);

        IntentFilter syncFilter = new IntentFilter(SyncService.SYNC_COMPLETE);
        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadData();
                    }
                });
            }
        };
        registerReceiver(_syncReceiver, syncFilter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(_syncReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setSelectedNavigationItem(2);

        loadData();

        this.invalidateOptionsMenu();

        SyncService.startSync(this);
    }

    private void setUpSwipe() {
        final View container = findViewById(R.id.category_week_container);
        View v = findViewById(R.id.week_chart);

        container.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                weekBack();
            }

            public void onSwipeLeft() {
                weekForward();
            }

            @SuppressLint("ClickableViewAccessibility")
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

            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });

        v = findViewById(R.id.category_week_expense_list);
        v.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                weekBack();
            }

            public void onSwipeLeft() {
                weekForward();
            }

            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    private String getPeriod() {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        while ((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 6);
        return Dates.getShortDateString(this, start.getTime()) + " - " + Dates.getShortDateString(this, end.getTime());
    }

    private void loadData() {
        _budget = Settings.getBudget(this);

        hideDetails();

        TextView dates = findViewById(R.id.category_current_week);
        dates.setText(getPeriod());

        List<CategoryAmount> list = DBHelper.GetCategoryAmountsForWeek(_budget.UniqueId, _daysBackFromToday, _budget.StartDay, this.getString(R.string.uncategorized));

        PieChart chart = findViewById(R.id.week_chart);

        List<Entry> entries = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            CategoryAmount categoryAmount = list.get(i);
            Entry e = new Entry((float) categoryAmount.Amount, i, categoryAmount);
            entries.add(e);
            names.add(categoryAmount.Name);
        }

        PieDataSet pds = new PieDataSet(entries, "");

        ArrayList<Integer> colors = new ArrayList<>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        pds.setColors(colors);

        PieData data = new PieData(names, pds);
        data.setValueFormatter(this);
        chart.setData(data);

        chart.invalidate();
    }

    public void weekBackOnClick(View v) {
        weekBack();
    }

    private void weekBack() {
        _daysBackFromToday += 7;
        loadData();
    }

    public void weekForwardOnClick(View v) {
        weekForward();
    }

    private void weekForward() {
        _daysBackFromToday -= 7;
        loadData();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        if (position == 0) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_WEEK);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 1) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_MONTH);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 3) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_CATEGORY_MONTH);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 4) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_CATEGORY);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.month, menu);
        if (_budget != null) {
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
        if (id == R.id.action_current_budget) {
            if (_budget != null) {
                try {
                    Intent i = new Intent(this, SwitchBudgetActivity.class);
                    int SWITCH_BUDGET = 2;
                    startActivityForResult(i, SWITCH_BUDGET);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        if (id == R.id.action_settings) {
            if (_budget != null) {
                try {
                    Intent i = new Intent(this, NewBudgetActivity.class);
                    i.putExtra("budget", _budget.toJson(false).toString());
                    int EDIT_BUDGET = 1;
                    startActivityForResult(i, EDIT_BUDGET);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return Helpers.currencyString(value);
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        CategoryAmount c = (CategoryAmount) e.getData();
        TextView tv = findViewById(R.id.category_week_selection_name);
        tv.setText(c.Name);
        tv.setVisibility(View.VISIBLE);

        NonScrollableListView lv = findViewById(R.id.category_week_expense_list);

        List<Expense> expenses = DBHelper.GetExpensesForCategoryForWeek(_budget.UniqueId, c.CategoryId != null ? c.CategoryId.toString() : null, _daysBackFromToday, _budget.StartDay);

        WeekRowAdapter aa = new WeekRowAdapter(this, R.layout.week_row, expenses);
        lv.setAdapter(aa);

        lv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNothingSelected() {
        hideDetails();
    }

    private void hideDetails() {
        TextView tv = findViewById(R.id.category_week_selection_name);
        tv.setVisibility(View.GONE);
        NonScrollableListView lv = findViewById(R.id.category_week_expense_list);
        lv.setVisibility(View.GONE);
        PieChart chart = findViewById(R.id.week_chart);
        chart.highlightValues(new Highlight[0]);
    }

    private void setUpOnLongClick() {
        ListView lv = this.findViewById(R.id.category_week_expense_list);
        registerForContextMenu(lv);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ListView lv = (ListView) v;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        lv.setTag(lv.getAdapter().getItem(info.position));
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.category_week_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListView lv = findViewById(R.id.category_week_expense_list);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Expense e = (Expense) lv.getItemAtPosition(info.position);

        try {
            switch (item.getItemId()) {
                case R.id.context_edit:
                    Intent i = new Intent(this, AddExpenseActivity.class);
                    i.putExtra("expense", e.toJson().toString());
                    this.startActivity(i);
                    break;
                case R.id.context_delete:
                    deleteExpense(e);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    private void deleteExpense(Expense e) {
        if (e.State.equals(DBHelper.CREATED_STATE_KEY))
            DBHelper.DeleteExpense(e);
        else
            DBHelper.EditExpense(e, DBHelper.DELETED_STATE_KEY);
        loadData();
        SyncService.startSync(this);
    }
}
