package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CategoryMonthActivity extends BaseActivity
        implements ActionBar.OnNavigationListener, OnChartValueSelectedListener {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;
    private int _daysBackFromToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_month);

        Navigation.setUp(this, this, Navigation.CATEGORY_MONTH);

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

        PieChart chart = findViewById(R.id.month_chart);
        CategoryChart.configure(chart);
        chart.setOnChartValueSelectedListener(this);

        NonScrollableListView lv = findViewById(R.id.category_month_expense_list);
        lv.setFocusable(false);

        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(CategoryMonthActivity.this::loadData);
            }
        };
        Sync.registerCompletionReceiver(this, _syncReceiver);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(_syncReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Navigation.select(this, Navigation.CATEGORY_MONTH);

        loadData();

        this.invalidateOptionsMenu();

        Sync.start(this);
    }

    private void setUpSwipe() {
        int[] swipeableIds = {R.id.category_month_container, R.id.month_chart,
                R.id.category_month_expense_list};

        for (int id : swipeableIds) {
            final boolean isContainer = id == R.id.category_month_container;
            findViewById(id).setOnTouchListener(new OnSwipeTouchListener(this) {
                public void onSwipeRight() {
                    monthBack();
                }

                public void onSwipeLeft() {
                    monthForward();
                }

                @SuppressLint("ClickableViewAccessibility")
                public boolean onTouch(View v, MotionEvent event) {
                    gestureDetector.onTouchEvent(event);
                    // Only the background consumes the gesture; the chart and
                    // the list still need their own taps.
                    return isContainer;
                }
            });
        }
    }

    private void loadData() {
        _budget = Settings.getBudget(this);

        if (_budget == null) {
            this.finish();
            return;
        }

        hideDetails();

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);

        TextView month = findViewById(R.id.category_current_month);
        month.setText(now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()));

        List<CategoryAmount> list = DBHelper.GetCategoryAmountsForMonth(_budget.UniqueId,
                _daysBackFromToday, this.getString(R.string.uncategorized));

        CategoryChart.populate(findViewById(R.id.month_chart), list);
    }

    public void monthBackOnClick(View v) {
        monthBack();
    }

    private void monthBack() {
        shiftMonths(-1);
    }

    public void monthForwardOnClick(View v) {
        monthForward();
    }

    private void monthForward() {
        if (_daysBackFromToday > 0) {
            shiftMonths(1);
        }
    }

    private void shiftMonths(int months) {
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar) now.clone();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        start.add(Calendar.MONTH, months);
        _daysBackFromToday = Dates.daysBetween(start, now);
        if (_daysBackFromToday < 0)
            _daysBackFromToday = 0;

        loadData();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        return ScreenSwitcher.onNavigationItemSelected(this, Navigation.CATEGORY_MONTH, position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.month, menu);
        if (_budget != null) {
            MenuItem s = menu.findItem(R.id.action_current_budget);
            s.setTitle(getString(R.string.current_budget_named, _budget.Name));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ScreenSwitcher.onOptionsItemSelected(this, item, _budget)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        CategoryAmount c = (CategoryAmount) e.getData();
        TextView tv = findViewById(R.id.category_month_selection_name);
        tv.setText(c.Name);
        tv.setVisibility(View.VISIBLE);

        NonScrollableListView lv = findViewById(R.id.category_month_expense_list);

        List<Expense> expenses = DBHelper.GetExpensesForCategoryForMonth(_budget.UniqueId,
                c.CategoryId != null ? c.CategoryId.toString() : null, _daysBackFromToday);

        WeekRowAdapter aa = new WeekRowAdapter(this, R.layout.week_row, expenses);
        lv.setAdapter(aa);

        lv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNothingSelected() {
        hideDetails();
    }

    private void hideDetails() {
        TextView tv = findViewById(R.id.category_month_selection_name);
        tv.setVisibility(View.GONE);
        NonScrollableListView lv = findViewById(R.id.category_month_expense_list);
        lv.setVisibility(View.GONE);
        CategoryChart.clearHighlights(findViewById(R.id.month_chart));
    }

    private void setUpOnLongClick() {
        ListView lv = this.findViewById(R.id.category_month_expense_list);
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
        inflater.inflate(R.menu.category_month_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListView lv = findViewById(R.id.category_month_expense_list);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Expense e = (Expense) lv.getItemAtPosition(info.position);

        try {
            int id = item.getItemId();
            if (id == R.id.context_edit) {
                Intent i = new Intent(this, AddExpenseActivity.class);
                i.putExtra("expense", e.toJson().toString());
                this.startActivity(i);
            } else if (id == R.id.context_delete) {
                deleteExpense(e);
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
        Sync.start(this);
    }
}
