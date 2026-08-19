package com.andrewovens.weeklybudget2;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

        Transitions.disableCloseAnimation(this);
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
        findViewById(R.id.current_month).setOnClickListener(v ->
                PeriodPicker.show(this, R.string.pick_month, _daysBackFromToday, true,
                        days -> {
                            _daysBackFromToday = days;
                            loadData();
                            invalidateOptionsMenu();
                        }));

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

    private void setUpSwipe() {
        PeriodSwipeLayout swipe = findViewById(R.id.month_swipe);
        swipe.setListener(new PeriodSwipeLayout.Listener() {
            @Override
            public void onNext() {
                shiftMonths(1);
            }

            @Override
            public void onPrevious() {
                shiftMonths(-1);
            }
        });
    }

    private void loadData() {
        _budget = Settings.getBudget(this);

        if (_budget == null) {
            this.finish();
            return;
        }

        BudgetTitle.asTitle(this, _budget);

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

        // A month with nothing in it still has week rows — one per week, each
        // reading zero — so showing the empty state on top of them stacked two
        // messages over each other. The list goes away with them.
        boolean anySpending = amount != 0;
        for (DateTotal week : list) {
            if (week.Total != 0) {
                anySpending = true;
                break;
            }
        }

        View empty = findViewById(R.id.month_empty);
        findViewById(R.id.month_list).setVisibility(anySpending ? View.VISIBLE : View.GONE);
        if (anySpending) {
            empty.setVisibility(View.GONE);
        } else {
            EmptyState.show(empty, R.drawable.ic_nav_month, R.string.month_empty_title,
                    R.string.month_empty_body);
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
        invalidateOptionsMenu();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem today = menu.findItem(R.id.action_today);
        if (today != null) {
            today.setVisible(_daysBackFromToday != 0);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_today) {
            _daysBackFromToday = 0;
            loadData();
            invalidateOptionsMenu();
            return true;
        }
        return ScreenSwitcher.onOptionsItemSelected(this, item, _budget)
                || super.onOptionsItemSelected(item);
    }
}
