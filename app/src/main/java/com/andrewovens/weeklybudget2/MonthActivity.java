package com.andrewovens.weeklybudget2;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.widget.AdapterView.OnItemClickListener;

import org.json.JSONObject;

import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

public class MonthActivity extends Activity implements OnNavigationListener {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;
    private int _daysBackFromToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month);

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
                        R.layout.main_menu_item,
                        R.id.main_menu_item_text,
                        new String[]{
                                getString(R.string.title_week),
                                getString(R.string.title_month),
                                getString(R.string.title_category_week),
                                getString(R.string.title_category_month),
                                getString(R.string.title_category),
                        }),
                this);

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
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MonthRowAdapter adapter = (MonthRowAdapter) parent.getAdapter();
                DateTotal dt = adapter.get(position);
                int daysBackFromToday = (int) ((Calendar.getInstance().getTimeInMillis() - dt.Date.getTimeInMillis()) / (24 * 60 * 60 * 1000));
                Intent i = new Intent(MonthActivity.this, WeekActivity.class);
                i.putExtra("days", daysBackFromToday);
                MonthActivity.this.setResult(Activity.RESULT_OK, i);
                MonthActivity.this.finish();
            }
        });

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
        actionBar.setSelectedNavigationItem(1);

        loadData();

        this.invalidateOptionsMenu();

        SyncService.startSync(this);
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

        assert _budget != null;
        int startDay = _budget.StartDay;

        Calendar start = Calendar.getInstance();
        while ((start.get(Calendar.DAY_OF_WEEK) - 1) != startDay) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }

        View headingsRowView = findViewById(R.id.month_headings);

        TextView day1 = headingsRowView.findViewById(R.id.month_row_day1);
        day1.setText(Dates.getWeekDay(start.getTime()));
        start.add(Calendar.DAY_OF_YEAR, 1);
        TextView day2 = headingsRowView.findViewById(R.id.month_row_day2);
        day2.setText(Dates.getWeekDay(start.getTime()));
        start.add(Calendar.DAY_OF_YEAR, 1);
        TextView day3 = headingsRowView.findViewById(R.id.month_row_day3);
        day3.setText(Dates.getWeekDay(start.getTime()));
        start.add(Calendar.DAY_OF_YEAR, 1);
        TextView day4 = headingsRowView.findViewById(R.id.month_row_day4);
        day4.setText(Dates.getWeekDay(start.getTime()));
        start.add(Calendar.DAY_OF_YEAR, 1);
        TextView day5 = headingsRowView.findViewById(R.id.month_row_day5);
        day5.setText(Dates.getWeekDay(start.getTime()));
        start.add(Calendar.DAY_OF_YEAR, 1);
        TextView day6 = headingsRowView.findViewById(R.id.month_row_day6);
        day6.setText(Dates.getWeekDay(start.getTime()));
        start.add(Calendar.DAY_OF_YEAR, 1);
        TextView day7 = headingsRowView.findViewById(R.id.month_row_day7);
        day7.setText(Dates.getWeekDay(start.getTime()));

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
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar) now.clone();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        start.add(Calendar.MONTH, -1);
        _daysBackFromToday = (int) ((now.getTimeInMillis() - start.getTimeInMillis()) / (24 * 60 * 60 * 1000));
        loadData();
    }

    public void monthForwardOnClick(View v) {
        monthForward();
    }

    private void monthForward() {
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar) now.clone();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        start.add(Calendar.MONTH, 1);
        _daysBackFromToday = (int) ((now.getTimeInMillis() - start.getTimeInMillis()) / (24 * 60 * 60 * 1000));
        if (_daysBackFromToday < 0)
            _daysBackFromToday = 0;

        loadData();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        if (position == 0) {
            try {
                Intent i = new Intent(this, WeekActivity.class);
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_WEEK);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 2) {
            try {
                Intent i = new Intent(this, WeekActivity.class);
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_CATEGORY_WEEK);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 3) {
            try {
                Intent i = new Intent(this, WeekActivity.class);
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_CATEGORY_MONTH);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 4) {
            try {
                Intent i = new Intent(this, WeekActivity.class);
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

    public class MonthRowAdapter extends ArrayAdapter<DateTotal> {
        private final Context context;
        private final int resourceID;
        private List<DateTotal> list;

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

            TextView day1 = rowView.findViewById(R.id.month_row_day1);
            day1.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));
            current.add(Calendar.DAY_OF_YEAR, 1);
            TextView day2 = rowView.findViewById(R.id.month_row_day2);
            day2.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));
            current.add(Calendar.DAY_OF_YEAR, 1);
            TextView day3 = rowView.findViewById(R.id.month_row_day3);
            day3.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));
            current.add(Calendar.DAY_OF_YEAR, 1);
            TextView day4 = rowView.findViewById(R.id.month_row_day4);
            day4.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));
            current.add(Calendar.DAY_OF_YEAR, 1);
            TextView day5 = rowView.findViewById(R.id.month_row_day5);
            day5.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));
            current.add(Calendar.DAY_OF_YEAR, 1);
            TextView day6 = rowView.findViewById(R.id.month_row_day6);
            day6.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));
            current.add(Calendar.DAY_OF_YEAR, 1);
            TextView day7 = rowView.findViewById(R.id.month_row_day7);
            day7.setText(NumberFormat.getInstance().format(current.get(Calendar.DAY_OF_MONTH)));

            TextView total = rowView.findViewById(R.id.month_row_total);
            total.setText(Helpers.currencyString(dt.Total));
            if (dt.Total > _budget.Amount)
                total.setTextColor(Color.RED);
            else
                total.setTextColor(Color.BLACK);

            return rowView;
        }

        DateTotal get(int position) {
            return list.get(position);
        }

    }
}
