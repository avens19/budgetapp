package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The category breakdown, shared by the by-week and by-month screens.
 *
 * <p>The two used to be near-identical copies of 270 lines each. They differ
 * only in the period they total over and how the arrows move it, so everything
 * else — the chart, the legend, the drill-down list, the menu — lives here and
 * each subclass supplies the period.
 *
 * <p>The legend below the chart is the substantive addition: a pie can show
 * proportions but cannot label a 3% slice, so every category is also a row
 * with its colour, name, share and amount, and selecting either the slice or
 * the row drills into that category's expenses.
 */
abstract class CategoryChartActivity extends BaseActivity
        implements Navigation.OnDestinationSelected, OnChartValueSelectedListener,
        ExpenseRow.Actions {

    Budget _budget;
    int _daysBackFromToday;

    private BroadcastReceiver _syncReceiver;
    private List<CategoryAmount> _amounts = new ArrayList<>();
    private CategoryIndex _categories;
    private Integer _selectedIndex;
    private boolean _bindingSegments;

    /** {@link Navigation#CATEGORY_WEEK} or {@link Navigation#CATEGORY_MONTH}. */
    abstract int navPosition();

    /** The heading between the arrows, e.g. "Aug 10 – Aug 16" or "August". */
    abstract String periodLabel();

    abstract List<CategoryAmount> loadAmounts();

    abstract List<Expense> loadExpensesFor(@Nullable Long categoryId);

    /** {@code -1} for the previous period, {@code +1} for the next. */
    abstract void shiftPeriod(int direction);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        Navigation.setUp(this, navPosition(), this);

        try {
            Intent i = getIntent();
            _budget = Budget.fromJson(new JSONObject(i.getStringExtra("budget")));
            _daysBackFromToday = i.getIntExtra("days", 0);
        } catch (Exception e) {
            this.finish();
            e.printStackTrace();
            return;
        }

        PieChart chart = findViewById(R.id.category_chart);
        CategoryChart.configure(chart);
        chart.setOnChartValueSelectedListener(this);

        findViewById(R.id.period_back).setOnClickListener(v -> move(-1));
        findViewById(R.id.period_forward).setOnClickListener(v -> move(1));

        setUpSegments();
        setUpSwipe();

        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(CategoryChartActivity.this::loadData);
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

        Navigation.select(this, navPosition());

        loadData();

        this.invalidateOptionsMenu();

        Sync.start(this);
    }

    private void setUpSegments() {
        MaterialButtonToggleGroup group = findViewById(R.id.segment_toggle);

        _bindingSegments = true;
        group.check(navPosition() == Navigation.CATEGORY_WEEK
                ? R.id.segment_week : R.id.segment_month);
        _bindingSegments = false;

        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked || _bindingSegments) {
                return;
            }
            int target = checkedId == R.id.segment_week
                    ? Navigation.CATEGORY_WEEK : Navigation.CATEGORY_MONTH;
            if (target != navPosition()) {
                ScreenSwitcher.goToPosition(this, target);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpSwipe() {
        // Only the background consumes the gesture; the chart and the rows
        // still need their own taps.
        int[] swipeableIds = {R.id.category_container, R.id.category_chart, R.id.category_scroll};

        for (int id : swipeableIds) {
            final boolean isContainer = id == R.id.category_container;
            findViewById(id).setOnTouchListener(new OnSwipeTouchListener(this) {
                public void onSwipeRight() {
                    move(-1);
                }

                public void onSwipeLeft() {
                    move(1);
                }

                public boolean onTouch(View v, MotionEvent event) {
                    gestureDetector.onTouchEvent(event);
                    return isContainer;
                }
            });
        }
    }

    private void move(int direction) {
        shiftPeriod(direction);
        loadData();
    }

    void loadData() {
        _budget = Settings.getBudget(this);

        if (_budget == null) {
            this.finish();
            return;
        }

        ((TextView) findViewById(R.id.period_label)).setText(periodLabel());

        _categories = CategoryIndex.of(this, DBHelper.GetActiveCategories(_budget.UniqueId, null));

        _amounts = loadAmounts();
        _selectedIndex = null;

        double total = 0;
        for (CategoryAmount a : _amounts) {
            total += a.Amount;
        }

        PieChart chart = findViewById(R.id.category_chart);
        View empty = findViewById(R.id.category_empty);
        View hint = findViewById(R.id.category_hint);

        if (_amounts.isEmpty()) {
            CategoryChart.clear(chart);
            chart.setVisibility(View.GONE);
            hint.setVisibility(View.GONE);
            EmptyState.show(empty, R.drawable.ic_nav_categories,
                    R.string.category_empty_title, R.string.category_empty_body);
        } else {
            chart.setVisibility(View.VISIBLE);
            hint.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
            CategoryChart.populate(chart, _amounts, Helpers.currencyString(total), _categories);
        }

        bindLegend(total);
        hideDetails();
    }

    private void bindLegend(double total) {
        LinearLayout container = findViewById(R.id.legend_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        while (container.getChildCount() > _amounts.size()) {
            container.removeViewAt(container.getChildCount() - 1);
        }
        while (container.getChildCount() < _amounts.size()) {
            container.addView(inflater.inflate(R.layout.item_category_legend, container, false));
        }

        for (int i = 0; i < _amounts.size(); i++) {
            final int index = i;
            CategoryAmount amount = _amounts.get(i);
            View row = container.getChildAt(i);

            ((TextView) row.findViewById(R.id.legend_name)).setText(amount.Name);
            ((TextView) row.findViewById(R.id.legend_amount))
                    .setText(Helpers.currencyString(amount.Amount));

            int share = total > 0 ? (int) Math.round(amount.Amount / total * 100) : 0;
            ((TextView) row.findViewById(R.id.legend_share))
                    .setText(getString(R.string.category_share, String.valueOf(share)));

            ImageView dot = row.findViewById(R.id.legend_dot);
            ImageViewCompat.setImageTintList(dot,
                    ColorStateList.valueOf(_categories.colorFor(amount.CategoryId)));

            row.setOnClickListener(v -> {
                if (_selectedIndex != null && _selectedIndex == index) {
                    CategoryChart.clearHighlights(findViewById(R.id.category_chart));
                    hideDetails();
                } else {
                    CategoryChart.highlight(findViewById(R.id.category_chart), index);
                    showDetails(index);
                }
            });
        }
    }

    private void showDetails(int index) {
        _selectedIndex = index;
        CategoryAmount selected = _amounts.get(index);

        TextView header = findViewById(R.id.selection_header);
        header.setText(selected.Name);
        header.setVisibility(View.VISIBLE);

        findViewById(R.id.category_hint).setVisibility(View.GONE);

        ViewGroup list = findViewById(R.id.selection_list);
        ExpenseRow.fill(list, loadExpensesFor(selected.CategoryId), _categories,
                ExpenseRow.Subtitle.DATE, false, this);
        list.setVisibility(View.VISIBLE);

        markLegendSelection(index);
    }

    private void hideDetails() {
        _selectedIndex = null;
        findViewById(R.id.selection_header).setVisibility(View.GONE);
        findViewById(R.id.selection_list).setVisibility(View.GONE);
        findViewById(R.id.category_hint).setVisibility(
                _amounts.isEmpty() ? View.GONE : View.VISIBLE);
        markLegendSelection(-1);
    }

    /**
     * Outlines the selected legend row. MaterialCardView's own checked state
     * only swaps a corner icon, which is too quiet to pair with a highlighted
     * chart slice, so the stroke and fill are set directly.
     */
    private void markLegendSelection(int index) {
        LinearLayout container = findViewById(R.id.legend_container);

        int selectedStroke = MaterialColors.getColor(container,
                androidx.appcompat.R.attr.colorPrimary, 0);
        int idleStroke = MaterialColors.getColor(container,
                com.google.android.material.R.attr.colorOutlineVariant, 0);
        int selectedFill = MaterialColors.getColor(container,
                com.google.android.material.R.attr.colorPrimaryContainer, 0);
        int idleFill = MaterialColors.getColor(container,
                com.google.android.material.R.attr.colorSurfaceContainerLowest, 0);

        int strokePx = Math.round(getResources().getDisplayMetrics().density * 2);

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof MaterialCardView)) {
                continue;
            }
            MaterialCardView card = (MaterialCardView) child;
            boolean selected = i == index;
            card.setStrokeColor(selected ? selectedStroke : idleStroke);
            card.setStrokeWidth(selected ? strokePx : strokePx / 2);
            card.setCardBackgroundColor(selected ? selectedFill : idleFill);
        }
    }

    // ---- Chart selection ---------------------------------------------------

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        CategoryAmount selected = (CategoryAmount) e.getData();
        for (int i = 0; i < _amounts.size(); i++) {
            if (_amounts.get(i) == selected) {
                showDetails(i);
                return;
            }
        }
    }

    @Override
    public void onNothingSelected() {
        hideDetails();
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
        // Not offered on these screens; bind() is called with allowCopy false.
    }

    // ---- Menu and navigation ----------------------------------------------

    @Override
    public void onDestinationSelected(int position) {
        ScreenSwitcher.goToPosition(this, position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.category, menu);
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
