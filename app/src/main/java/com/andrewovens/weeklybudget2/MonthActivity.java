package com.andrewovens.weeklybudget2;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

public class MonthActivity extends BaseActivity implements ActionBar.OnNavigationListener {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;
    private int _daysBackFromToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month);

        Navigation.setUp(this, this, Navigation.MONTH);

        setUpSwipe();

        try {
            Intent i = getIntent();
            String budgetString = i.getStringExtra("budget");
            _budget = Budget.fromJson(new JSONObject(budgetString));
            _daysBackFromToday = i.getIntExtra("days", 0);
        } catch (Exception e) {
            this.finish();
            e.printStackTrace();
        }

        ListView lv = MonthActivity.this.findViewById(R.id.month_list);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            MonthRowAdapter adapter = (MonthRowAdapter) parent.getAdapter();
            DateTotal dt = adapter.get(position);
            Intent i = new Intent(MonthActivity.this, WeekActivity.class);
            i.putExtra("days", Dates.daysBetween(dt.Date, Calendar.getInstance()));
            MonthActivity.this.setResult(Activity.RESULT_OK, i);
            MonthActivity.this.finish();
        });

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
        final View container = findViewById(R.id.month_container);
        View v = findViewById(R.id.month_list);

        container.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                monthBack();
            }

            public void onSwipeLeft() {
                monthForward();
            }

            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
        v.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                monthBack();
            }

            public void onSwipeLeft() {
                monthForward();
            }

            @SuppressLint("ClickableViewAccessibility")
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

        int startDay = _budget.StartDay;

        Calendar start = Calendar.getInstance();
        while ((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }

        View headingsRowView = findViewById(R.id.month_headings);

        int[] dayIds = {R.id.month_row_day1, R.id.month_row_day2, R.id.month_row_day3,
                R.id.month_row_day4, R.id.month_row_day5, R.id.month_row_day6, R.id.month_row_day7};
        for (int dayId : dayIds) {
            TextView day = headingsRowView.findViewById(dayId);
            day.setText(Dates.getWeekDay(start.getTime()));
            start.add(Calendar.DAY_OF_YEAR, 1);
        }

        TextView totalHeading = headingsRowView.findViewById(R.id.month_row_total);
        totalHeading.setText(getString(R.string.month_activity_total));

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);

        TextView month = findViewById(R.id.current_month);
        month.setText(now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()));

        List<DateTotal> list = DBHelper.GetTotalsForMonth(_budget.UniqueId, _daysBackFromToday, _budget.StartDay);

        ListView lv = MonthActivity.this.findViewById(R.id.month_list);
        MonthRowAdapter _adapter = new MonthRowAdapter(MonthActivity.this, R.layout.month_row, list);
        lv.setAdapter(_adapter);

        View rowView = findViewById(R.id.month_total_row);
        TextView total = rowView.findViewById(R.id.month_total_row_total);

        double amount = DBHelper.GetTotalForMonth(_budget.UniqueId, _daysBackFromToday);

        total.setText(Helpers.currencyString(amount));
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
        shiftMonths(1);
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
        return ScreenSwitcher.onNavigationItemSelected(this, Navigation.MONTH, position);
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

    public class MonthRowAdapter extends ArrayAdapter<DateTotal> {
        private final Context context;
        private final int resourceID;
        private final List<DateTotal> list;

        MonthRowAdapter(Context context, int resource, List<DateTotal> bah) {
            super(context, resource, bah);

            this.context = context;
            this.resourceID = resource;
            this.list = bah;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = convertView != null ? convertView : inflater.inflate(resourceID, parent, false);

            DateTotal dt = list.get(position);

            Calendar current = (Calendar) dt.Date.clone();

            int[] dayIds = {R.id.month_row_day1, R.id.month_row_day2, R.id.month_row_day3,
                    R.id.month_row_day4, R.id.month_row_day5, R.id.month_row_day6, R.id.month_row_day7};
            for (int dayId : dayIds) {
                TextView day = rowView.findViewById(dayId);
                day.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));
                current.add(Calendar.DAY_OF_YEAR, 1);
            }

            TextView total = rowView.findViewById(R.id.month_row_total);
            total.setText(Helpers.currencyString(dt.Total));
            total.setTextColor(ContextCompat.getColor(context, dt.Total > _budget.Amount
                    ? R.color.amount_over_budget
                    : R.color.amount_within_budget));

            return rowView;
        }

        DateTotal get(int position) {
            return list.get(position);
        }

    }
}
