package com.andrewovens.weeklybudget2;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class WeekActivity extends BaseActivity implements ActionBar.OnNavigationListener {

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

    /** Set by the home-screen widget to jump straight to the add-expense form. */
    public static final String EXTRA_ADD_EXPENSE = "ADD";

    public static final int GOTO_WEEK = 200;
    public static final int GOTO_MONTH = 201;
    public static final int GOTO_CATEGORY_WEEK = 202;
    public static final int GOTO_CATEGORY_MONTH = 203;
    public static final int GOTO_CATEGORY = 204;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week);

        Navigation.setUp(this, this, Navigation.WEEK);

        setUpSwipe();
        ListView lv = WeekActivity.this.findViewById(R.id.week_list);
        registerForContextMenu(lv);

        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final View spinner = findViewById(R.id.main_load);
                runOnUiThread(() -> {
                    if (spinner != null) {
                        spinner.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        Sync.registerCompletionReceiver(this, _syncReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // The widget reuses this task, so the "open the add form" extra arrives
        // here rather than in onCreate. loadData() reads getIntent().
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(_syncReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        DBHelper.OpenDB(this);

        Navigation.select(this, Navigation.WEEK);

        loadData();

        startSync();

        this.invalidateOptionsMenu();
    }

    private void startSync() {
        View spinner = findViewById(R.id.main_load);
        spinner.setVisibility(View.VISIBLE);
        Sync.start(this);
    }

    private void setUpSwipe() {
        final View container = findViewById(R.id.week_container);
        View v = findViewById(R.id.week_list);

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

        // Check for first run.
        if (_budget == null) {
            Intent k = new Intent(this, FirstActivity.class);
            startActivityForResult(k, FIRST_ACTIVITY);
            return;
        }

        Budget[] budgets = Settings.getBudgets(this);

        // Migration to add Budget name and support multiple budgets
        if (budgets == null) {
            migrateToNamedBudgets(_budget.UniqueId);
        }

        // Check for open from widget
        if (this.getIntent().getBooleanExtra(EXTRA_ADD_EXPENSE, false)) {
            this.getIntent().putExtra(EXTRA_ADD_EXPENSE, false);
            Intent i = new Intent(WeekActivity.this, AddExpenseActivity.class);
            startActivity(i);
            return;
        }

        List<Expense> expenses = DBHelper.GetExpensesForWeek(_budget.UniqueId, _daysBackFromToday, _budget.StartDay);

        double total = 0;
        for (int i = 0; i < expenses.size(); i++) {
            total += expenses.get(i).Amount;
        }
        double remaining = _budget.Amount - total;

        Button r = WeekActivity.this.findViewById(R.id.remaining);
        final double rounded = Math.round(remaining * 100) / 100.0;
        if (rounded >= 0) {
            r.setText(getString(R.string.week_activity_amount_remaining, Helpers.currencyString(rounded)));
            r.setTextColor(ContextCompat.getColor(this, R.color.amount_within_budget));
        } else {
            r.setText(getString(R.string.week_activity_amount_over, Helpers.currencyString(Math.abs(rounded))));
            r.setTextColor(ContextCompat.getColor(this, R.color.amount_over_budget));
        }
        r.setOnClickListener(v -> confirmCarryBalance(rounded));
        ListView lv = WeekActivity.this.findViewById(R.id.week_list);
        if (lv.getAdapter() == null) {
            WeekRowAdapter adapter = new WeekRowAdapter(WeekActivity.this, R.layout.week_row, expenses);
            lv.setAdapter(adapter);
        } else {
            ((WeekRowAdapter) lv.getAdapter()).clear();
            ((WeekRowAdapter) lv.getAdapter()).addAll(expenses);
        }

        TextView dates = findViewById(R.id.current_week);
        dates.setText(getPeriod());

        notifyWidgets();
    }

    /**
     * Backfills the budget list for installs that predate multiple-budget
     * support. The result is applied on the main thread so that the fields the
     * UI reads are never mutated from a background thread mid-layout.
     */
    private void migrateToNamedBudgets(final String uniqueId) {
        new Thread(() -> {
            try {
                final Budget budget = API.GetBudget(uniqueId);

                runOnUiThread(() -> {
                    try {
                        Settings.setBudget(WeekActivity.this, budget);
                        Settings.setBudgets(WeekActivity.this, new Budget[]{budget});
                        _budget = Budget.update(_budget, budget);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                Helpers.showNetworkErrorToastOnUi(WeekActivity.this, R.string.error_network);
                e.printStackTrace();
            }
        }).start();
    }

    private void confirmCarryBalance(final double rounded) {
        if (DBHelper.SystemExpenseExistsForWeek(_budget.UniqueId, _daysBackFromToday - 7, _budget.StartDay)) {
            Toast.makeText(WeekActivity.this, R.string.already_carried, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(WeekActivity.this);

        builder
                .setTitle(R.string.carry_balance)
                .setMessage(R.string.carry_balance_message)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Calendar start = Calendar.getInstance();
                    start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
                    while ((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay) {
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
                    DBHelper.AddExpense(e, DBHelper.CREATED_STATE_KEY);
                    loadData();
                    dialog.dismiss();
                });

        Dialog d = builder.create();

        d.show();
    }

    private void notifyWidgets() {
        Intent intent = new Intent(WeekActivity.this, AddExpenseWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AddExpenseWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
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
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ListView lv = (ListView) v;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        lv.setTag(lv.getAdapter().getItem(info.position));
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.week_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListView lv = findViewById(R.id.week_list);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Expense e = (Expense) lv.getItemAtPosition(info.position);

        try {
            int id = item.getItemId();
            if (id == R.id.context_edit) {
                Intent i = new Intent(WeekActivity.this, AddExpenseActivity.class);
                i.putExtra("expense", e.toJson().toString());
                WeekActivity.this.startActivity(i);
            } else if (id == R.id.context_delete) {
                deleteExpense(e);
            } else if (id == R.id.context_copy) {
                copyExpense(e);
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

    private void copyExpense(Expense e) {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        while ((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay) {
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
        DBHelper.AddExpense(ex, DBHelper.CREATED_STATE_KEY);
        loadData();
        Sync.start(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.week, menu);
        MenuItem add = menu.findItem(R.id.action_add);
        add.setOnMenuItemClickListener(arg0 -> {
            Intent i = new Intent(WeekActivity.this, AddExpenseActivity.class);
            startActivity(i);
            return true;
        });
        if (_budget != null) {
            MenuItem s = menu.findItem(R.id.action_current_budget);
            s.setTitle(getString(R.string.current_budget_named, _budget.Name));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_current_budget) {
            if (_budget != null) {
                Intent i = new Intent(this, SwitchBudgetActivity.class);
                startActivityForResult(i, SWITCH_BUDGET);
            }
            return true;
        }
        if (id == R.id.action_settings) {
            if (_budget != null) {
                try {
                    Intent i = new Intent(this, NewBudgetActivity.class);
                    i.putExtra("budget", _budget.toJson(false).toString());
                    i.putExtra("days", _daysBackFromToday);
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
    public boolean onNavigationItemSelected(int position, long id) {
        if (position == Navigation.WEEK || _budget == null) {
            return true;
        }
        if (position == Navigation.MONTH) {
            gotoMonth();
        } else if (position == Navigation.CATEGORY_WEEK) {
            gotoCategoryWeek();
        } else if (position == Navigation.CATEGORY_MONTH) {
            gotoCategoryMonth();
        } else if (position == Navigation.CATEGORY) {
            gotoCategory();
        }
        return true;
    }

    private void goTo(Class<?> target, int requestCode) {
        try {
            Intent i = new Intent(this, target);
            i.putExtra("budget", _budget.toJson(false).toString());
            i.putExtra("days", _daysBackFromToday);
            startActivityForResult(i, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gotoMonth() {
        goTo(MonthActivity.class, MONTH_ACTIVITY);
    }

    private void gotoCategoryWeek() {
        goTo(CategoryWeekActivity.class, CATEGORY_WEEK_ACTIVITY);
    }

    private void gotoCategoryMonth() {
        goTo(CategoryMonthActivity.class, CATEGORY_MONTH_ACTIVITY);
    }

    private void gotoCategory() {
        goTo(CategoryActivity.class, CATEGORY_ACTIVITY);
    }

    private void gotoActivity(int go) {
        switch (go) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FIRST_ACTIVITY && resultCode != Activity.RESULT_OK) {
            this.finish();
            return;
        }

        int days = -1;
        int activity = GOTO_WEEK;
        if (data != null) {
            days = data.getIntExtra("days", -1);
            activity = data.getIntExtra(GOTO_ACTIVITY, GOTO_WEEK);
        }
        if (days != -1) {
            _daysBackFromToday = days;
        }

        gotoActivity(activity);
    }
}
