package com.andrewovens.weeklybudget2;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

/** Category totals for one week. */
public class CategoryWeekActivity extends CategoryChartActivity {

    @Override
    int navPosition() {
        return Navigation.CATEGORY_WEEK;
    }

    @Override
    String periodLabel() {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        while ((start.get(Calendar.DAY_OF_WEEK) - 1) != _budget.StartDay) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 6);
        return getString(R.string.month_week_range,
                Dates.getShortDateString(this, start.getTime()),
                Dates.getShortDateString(this, end.getTime()));
    }

    @Override
    List<CategoryAmount> loadAmounts() {
        return DBHelper.GetCategoryAmountsForWeek(_budget.UniqueId, _daysBackFromToday,
                _budget.StartDay, getString(R.string.uncategorized));
    }

    @Override
    List<Expense> loadExpensesFor(@Nullable Long categoryId) {
        return DBHelper.GetExpensesForCategoryForWeek(_budget.UniqueId,
                categoryId != null ? categoryId.toString() : null,
                _daysBackFromToday, _budget.StartDay);
    }

    @Override
    void shiftPeriod(int direction) {
        _daysBackFromToday -= direction * 7;
    }
}
