package com.andrewovens.weeklybudget2;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

public class MonthActivity extends BaseActivity
        implements Navigation.OnDestinationSelected, WeekSummaryAdapter.OnWeekSelected {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;
    private int _daysBackFromToday;
    private WeekSummaryAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month);

        Navigation.setUp(this, Navigation.MONTH, this);

        try {
            Intent i = getIntent();
            String budgetString = i.getStringExtra("budget");
            _budget = Budget.fromJson(new JSONObject(budgetString));
            _daysBackFromToday = i.getIntExtra("days", 0);
        } catch (Exception e) {
            this.finish();
            e.printStackTrace();
            return;
        }

        _adapter = new WeekSummaryAdapter(this);
        RecyclerView list = findViewById(R.id.month_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(_adapter);

        findViewById(R.id.month_back).setOnClickListener(v -> shiftMonths(-1));
        findViewById(R.id.month_forward).setOnClickListener(v -> shiftMonths(1));

        setUpSwipe();

        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(MonthActivity.this::loadData);
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

        Navigation.select(this, Navigation.MONTH);

        loadData();

        this.invalidateOptionsMenu();

        Sync.start(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpSwipe() {
        findViewById(R.id.month_container).setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                shiftMonths(-1);
            }

            public void onSwipeLeft() {
                shiftMonths(1);
            }

            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

        findViewById(R.id.month_list).setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                shiftMonths(-1);
            }

            public void onSwipeLeft() {
                shiftMonths(1);
            }

            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    private void loadData() {
        _budget = Settings.getBudget(this);

        if (_budget == null) {
            this.finish();
            return;
        }

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);

        TextView month = findViewById(R.id.current_month);
        month.setText(now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()));

        List<DateTotal> list = DBHelper.GetTotalsForMonth(_budget.UniqueId, _daysBackFromToday, _budget.StartDay);
        _adapter.setWeeks(list, _budget.Amount);

        double amount = DBHelper.GetTotalForMonth(_budget.UniqueId, _daysBackFromToday);
        ((TextView) findViewById(R.id.month_total)).setText(Helpers.currencyString(amount));

        // Deliberately not an average: the headline is the calendar month,
        // while the cards below are whole weeks that can start in the previous
        // month, so month-total-over-week-count divides two different spans and
        // reads as a real figure. The weekly budget is the number the bars are
        // actually measured against.
        TextView budgetLine = findViewById(R.id.month_average);
        budgetLine.setText(getString(R.string.month_weekly_budget,
                Helpers.currencyString(_budget.Amount)));

        View empty = findViewById(R.id.month_empty);
        if (amount == 0) {
            EmptyState.show(empty, R.drawable.ic_nav_month, R.string.month_empty_title,
                    R.string.month_empty_body);
        } else {
            empty.setVisibility(View.GONE);
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
    public void onWeekSelected(DateTotal week) {
        Intent i = new Intent(this, WeekActivity.class);
        i.putExtra("days", Dates.daysBetween(week.Date, Calendar.getInstance()));
        setResult(Activity.RESULT_OK, i);
        finish();
    }

    @Override
    public void onDestinationSelected(int position) {
        ScreenSwitcher.goToPosition(this, position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
}
