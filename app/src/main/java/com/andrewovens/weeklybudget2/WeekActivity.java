package com.andrewovens.weeklybudget2;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class WeekActivity extends BaseActivity
        implements Navigation.OnDestinationSelected, ExpenseRow.Actions {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;
    private int _daysBackFromToday = 0;
    private ExpenseAdapter _adapter;

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

    /** Read once in onCreate; toggling it recreates the screen. */
    private boolean _dense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week);

        Navigation.setUp(this, Navigation.WEEK, this);

        _dense = Settings.isDenseLayout(this);

        collapseBalanceCard();

        _adapter = new ExpenseAdapter(this, true);
        _adapter.setDense(_dense);
        RecyclerView list = findViewById(R.id.week_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(_adapter);

        applyDensity();

        findViewById(R.id.week_back).setOnClickListener(v -> weekBack());
        findViewById(R.id.week_forward).setOnClickListener(v -> weekForward());
        findViewById(R.id.current_week).setOnClickListener(v ->
                PeriodPicker.show(this, R.string.pick_week, _daysBackFromToday, false,
                        days -> {
                            _daysBackFromToday = days;
                            loadData();
                        }));
        findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        setUpSwipe();

        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(() -> {
                    View spinner = findViewById(R.id.main_load);
                    if (spinner != null) {
                        spinner.setVisibility(View.INVISIBLE);
                    }
                    // A sync can bring in expenses from another device, so the
                    // week is re-read rather than only clearing the spinner.
                    if (_budget != null) {
                        loadData();
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
        LinearProgressIndicator spinner = findViewById(R.id.main_load);
        spinner.setVisibility(View.VISIBLE);
        Sync.start(this);
    }

    private void setUpSwipe() {
        PeriodSwipeLayout swipe = findViewById(R.id.week_swipe);
        swipe.setListener(new PeriodSwipeLayout.Listener() {
            @Override
            public void onNext() {
                weekForward();
            }

            @Override
            public void onPrevious() {
                weekBack();
            }
        });
    }

    /** The Sunday-to-Saturday (or whichever start day) week being shown. */
    private Calendar weekStart() {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        while ((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        return start;
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

        BudgetTitle.asTitle(this, _budget);

        List<Expense> expenses = DBHelper.GetExpensesForWeek(_budget.UniqueId, _daysBackFromToday, _budget.StartDay);

        double total = 0;
        for (Expense e : expenses) {
            total += e.Amount;
        }
        final double rounded = Math.round((_budget.Amount - total) * 100) / 100.0;

        bindBalance(total, rounded);
        bindPeriod();

        _adapter.setCategories(CategoryIndex.of(this,
                DBHelper.GetActiveCategories(_budget.UniqueId, null)));
        _adapter.setTotal(getString(R.string.week_spent_of,
                Helpers.currencyString(total), Helpers.currencyString(_budget.Amount)));
        _adapter.setExpenses(expenses);

        View empty = findViewById(R.id.week_empty);
        findViewById(R.id.week_list).setVisibility(expenses.isEmpty() ? View.GONE : View.VISIBLE);
        if (expenses.isEmpty()) {
            EmptyState.show(empty, R.drawable.ic_receipt, R.string.week_empty_title,
                    R.string.week_empty_body);
        } else {
            empty.setVisibility(View.GONE);
        }

        notifyWidgets();
    }

    /**
     * Takes the spent total out of the balance card and puts the number and the
     * Carry action on one line.
     *
     * <p>The total belongs under the table, beneath the things it adds up; the card
     * is for the one figure that answers "can I buy this", and it says that better
     * with less around it. Removing the total left Carry alone on a row that still
     * cost its full 48dp, which read as a hole in the card — so the number moves
     * onto that row, and a row disappears rather than emptying.
     *
     * <p>Both layouts, because both had the same hole.
     */
    private void collapseBalanceCard() {
        TextView spentOf = findViewById(R.id.spent_of);
        TextView amount = findViewById(R.id.remaining_amount);
        ViewGroup inner = (ViewGroup) ((ViewGroup) findViewById(R.id.balance_card)).getChildAt(0);
        ViewGroup summaryRow = (ViewGroup) spentOf.getParent();

        spentOf.setVisibility(View.GONE);

        // One line, so a long amount is truncated rather than wrapping the row and
        // undoing the saving. Realistic weekly figures are nowhere near the width.
        amount.setMaxLines(1);

        inner.removeView(amount);
        // Weighted, so the number takes the slack and pushes Carry to the edge.
        summaryRow.addView(amount, 0, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // The bar reads as a rule under the numbers it describes, which is also how
        // the older layout was arranged.
        inner.removeView(summaryRow);
        inner.addView(summaryRow, 1);

        ViewGroup.MarginLayoutParams rowParams =
                (ViewGroup.MarginLayoutParams) summaryRow.getLayoutParams();
        rowParams.topMargin = 0;
        summaryRow.setLayoutParams(rowParams);
    }

    /**
     * Shrinks the chrome for compact mode.
     *
     * <p>Done in code against the one layout rather than as a second XML file: the
     * two versions differ only in sizes and in one hidden label, and a duplicate
     * layout would drift the moment anything on this screen changed.
     */
    private void applyDensity() {
        if (!_dense) {
            return;
        }

        findViewById(R.id.remaining_label).setVisibility(View.GONE);

        TextView amount = findViewById(R.id.remaining_amount);
        amount.setTextAppearance(R.style.TextAppearance_Budget_Money_HeroDense);

        ViewGroup inner = (ViewGroup) ((ViewGroup) findViewById(R.id.balance_card)).getChildAt(0);

        // A MaterialButton is 48dp tall before it contains anything, and with the
        // card down to two lines that was setting the height on its own.
        MaterialButton carry = findViewById(R.id.carry_balance);
        carry.setMinHeight(0);
        carry.setMinimumHeight(0);
        carry.setInsetTop(0);
        carry.setInsetBottom(0);
        carry.setIconSize(getResources().getDimensionPixelSize(R.dimen.carry_icon_dense));
        carry.setTextAppearance(R.style.TextAppearance_Budget_CarryDense);

        int h = getResources().getDimensionPixelSize(R.dimen.space_m);
        int v = getResources().getDimensionPixelSize(R.dimen.space_s);
        inner.setPadding(h, v, h, v);

        View progress = findViewById(R.id.budget_progress);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) progress.getLayoutParams();
        lp.topMargin = getResources().getDimensionPixelSize(R.dimen.space_s);
        progress.setLayoutParams(lp);

        // Dense rows are separated by rules and want the full width; the gutter
        // that keeps cards clear of the edge is wasted on them.
        View list = findViewById(R.id.week_list);
        int gutter = getResources().getDimensionPixelSize(R.dimen.space_s);
        list.setPadding(gutter, list.getPaddingTop(), gutter, list.getPaddingBottom());
    }

    private void bindBalance(double spent, final double rounded) {
        TextView label = findViewById(R.id.remaining_label);
        TextView amount = findViewById(R.id.remaining_amount);
        TextView spentOf = findViewById(R.id.spent_of);
        LinearProgressIndicator progress = findViewById(R.id.budget_progress);
        MaterialCardView card = findViewById(R.id.balance_card);

        boolean over = rounded < 0;

        label.setText(over ? R.string.week_over_label : R.string.week_remaining_label);
        // Compact mode hides the label, so the word moves onto the number itself
        // rather than being lost: "$50.35 over" reads the same as the two lines
        // it replaces, in a third of the height.
        amount.setText(_dense
                ? getString(over ? R.string.week_dense_over : R.string.week_dense_left,
                            Helpers.currencyString(Math.abs(rounded)))
                : Helpers.currencyString(Math.abs(rounded)));
        spentOf.setText(getString(R.string.week_spent_of,
                Helpers.currencyString(spent), Helpers.currencyString(_budget.Amount)));

        // Overspending recolours the whole card rather than only the number:
        // it is the one state on this screen worth interrupting for.
        int container = MaterialColors.getColor(card, over
                ? com.google.android.material.R.attr.colorErrorContainer
                : com.google.android.material.R.attr.colorPrimaryContainer);
        int onContainer = MaterialColors.getColor(card, over
                ? com.google.android.material.R.attr.colorOnErrorContainer
                : com.google.android.material.R.attr.colorOnPrimaryContainer);

        card.setCardBackgroundColor(container);
        label.setTextColor(onContainer);
        amount.setTextColor(onContainer);
        spentOf.setTextColor(onContainer);

        MaterialButton carry = findViewById(R.id.carry_balance);
        carry.setTextColor(onContainer);
        carry.setIconTint(android.content.res.ColorStateList.valueOf(onContainer));
        carry.setOnClickListener(v -> confirmCarryBalance(rounded));

        int pct = _budget.Amount > 0
                ? (int) Math.round(Math.min(spent / _budget.Amount, 1.0) * 100)
                : 0;
        progress.setProgressCompat(Math.max(pct, 0), true);
        progress.setIndicatorColor(over
                ? MaterialColors.getColor(card, androidx.appcompat.R.attr.colorError)
                : MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary));
    }

    private void bindPeriod() {
        Calendar start = weekStart();
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 6);

        TextView dates = findViewById(R.id.current_week);
        dates.setText(getString(R.string.month_week_range,
                Dates.getShortDateString(this, start.getTime()),
                Dates.getShortDateString(this, end.getTime())));

        TextView subtitle = findViewById(R.id.week_subtitle);
        Calendar today = Calendar.getInstance();
        int daysLeft = Dates.daysBetween(today, end);
        boolean isCurrentWeek = Dates.daysBetween(start, today) >= 0 && daysLeft >= 0;

        if (!isCurrentWeek) {
            subtitle.setVisibility(View.GONE);
            return;
        }

        String remaining = daysLeft == 0
                ? getString(R.string.week_last_day)
                : getResources().getQuantityString(R.plurals.week_days_left, daysLeft, daysLeft);
        subtitle.setText(getString(R.string.week_subtitle,
                getString(R.string.week_this_week), remaining));
        subtitle.setVisibility(View.VISIBLE);
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

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.carry_balance)
                .setMessage(getString(R.string.carry_balance_message, Helpers.currencyString(rounded)))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Calendar start = weekStart();
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
                })
                .show();
    }

    private void notifyWidgets() {
        Intent intent = new Intent(WeekActivity.this, AddExpenseWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AddExpenseWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void weekBack() {
        _daysBackFromToday += 7;
        loadData();
        invalidateOptionsMenu();
    }

    private void weekForward() {
        _daysBackFromToday -= 7;
        loadData();
        invalidateOptionsMenu();
    }

    // ---- ExpenseRow.Actions ------------------------------------------------

    @Override
    public void onEdit(Expense e) {
        try {
            Intent i = new Intent(this, AddExpenseActivity.class);
            i.putExtra("expense", e.toJson().toString());
            startActivity(i);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onDelete(Expense e) {
        if (DBHelper.CREATED_STATE_KEY.equals(e.State)) {
            DBHelper.DeleteExpense(e);
        } else {
            DBHelper.EditExpense(e, DBHelper.DELETED_STATE_KEY);
        }
        loadData();
        Sync.start(this);
    }

    @Override
    public void onCopyToNextWeek(Expense e) {
        Calendar start = weekStart();
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

    // ---- Menu and navigation ----------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.week, menu);
        if (_budget != null) {
            MenuItem s = menu.findItem(R.id.action_current_budget);
            s.setTitle(getString(R.string.current_budget_named, _budget.Name));
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Only worth offering when it would actually move you.
        MenuItem today = menu.findItem(R.id.action_today);
        if (today != null) {
            today.setVisible(_daysBackFromToday != 0);
        }
        MenuItem dense = menu.findItem(R.id.action_dense_layout);
        if (dense != null) {
            dense.setChecked(_dense);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_today) {
            _daysBackFromToday = 0;
            loadData();
            invalidateOptionsMenu();
            return true;
        }
        if (id == R.id.action_current_budget) {
            if (_budget != null) {
                Intent i = new Intent(this, SwitchBudgetActivity.class);
                startActivityForResult(i, SWITCH_BUDGET);
            }
            return true;
        }
        if (id == R.id.action_dense_layout) {
            // Recreated rather than re-bound: the row layout and the grouping both
            // change, and a RecyclerView holding pooled views of the old shape
            // would hand them back for the new one.
            Settings.setDenseLayout(this, !_dense);
            recreate();
            return true;
        }
        if (id == R.id.action_how_it_works) {
            Intent i = new Intent(this, TutorialActivity.class);
            i.putExtra(TutorialActivity.EXTRA_STANDALONE, true);
            startActivity(i);
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
    public void onDestinationSelected(int position) {
        if (_budget == null) {
            return;
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
    }

    private void goTo(Class<?> target, int requestCode) {
        try {
            Intent i = new Intent(this, target);
            i.putExtra("budget", _budget.toJson(false).toString());
            i.putExtra("days", _daysBackFromToday);
            startActivityForResult(Transitions.noAnimation(i), requestCode);
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
