package com.andrewovens.weeklybudget2;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Category totals for one calendar month. */
public class CategoryMonthActivity extends CategoryChartActivity {

    @Override
    int navPosition() {
        return Navigation.CATEGORY_MONTH;
    }

    @Override
    String periodLabel() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        return now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }

    @Override
    List<CategoryAmount> loadAmounts() {
        return DBHelper.GetCategoryAmountsForMonth(_budget.UniqueId, _daysBackFromToday,
                getString(R.string.uncategorized));
    }

    @Override
    List<Expense> loadExpensesFor(@Nullable Long categoryId) {
        return DBHelper.GetExpensesForCategoryForMonth(_budget.UniqueId,
                categoryId != null ? categoryId.toString() : null, _daysBackFromToday);
    }

    @Override
    void shiftPeriod(int direction) {
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar) now.clone();
        start.add(Calendar.DAY_OF_YEAR, _daysBackFromToday * -1);
        start.add(Calendar.MONTH, direction);
        _daysBackFromToday = Dates.daysBetween(start, now);
        if (_daysBackFromToday < 0) {
            _daysBackFromToday = 0;
        }
    }
}
