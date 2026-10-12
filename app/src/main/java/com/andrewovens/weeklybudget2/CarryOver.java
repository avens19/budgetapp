package com.andrewovens.weeklybudget2;

import android.content.Context;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Moves last week's balance into the current week, once, for a budget that has
 * turned that on.
 *
 * <p>The Carry button does the same thing by hand, and people forget to press
 * it. A carry is a system expense on the first day of the week whose amount is
 * the negative of what was left, so a surplus lowers the week's spending and a
 * deficit (a positive amount) raises it. The web app and iOS write exactly the
 * same row, which is how all three read each other's carries.
 */
final class CarryOver {
    private CarryOver() {
    }

    /**
     * The amount of the carry expense, or 0 when there is nothing worth writing.
     *
     * <p>Under a cent either way is rounding noise, and a "Carried Balance" row
     * of $0.00 is clutter.
     */
    static double amountToCarry(double limit, List<Expense> week) {
        double spent = 0;
        for (Expense e : week) {
            spent += e.Amount;
        }
        double left = Math.round((limit - spent) * 100) / 100.0;
        return Math.abs(left) < 0.01 ? 0 : -left;
    }

    /**
     * Carries last week into this one if the budget has asked for that and it has
     * not been done yet.
     *
     * <p>Meant to run when a sync finishes, so that a carry another device
     * already made has arrived and is seen rather than made twice. A sync that
     * failed still counts: offline is the normal case, and the carry is written
     * locally and pushed with everything else.
     *
     * <p>Once per budget per week, remembered on the device, so a carry the
     * user deleted stays deleted: from the expenses alone it is
     * indistinguishable from one that was never made.
     *
     * @return whether an expense was written
     */
    static boolean carryIfDue(Context context, Budget budget) {
        if (!budget.CarryBalance) {
            return false;
        }

        String thisWeek = DBHelper.WeekStartString(0, budget.StartDay);
        if (thisWeek.equals(Settings.getCarriedWeek(context, budget.UniqueId))) {
            return false;
        }

        // The Carry button, or another device, got there first.
        if (DBHelper.SystemExpenseExistsForWeek(budget.UniqueId, 0, budget.StartDay)) {
            Settings.setCarriedWeek(context, budget.UniqueId, thisWeek);
            return false;
        }

        // A budget with nothing before this week has nothing to carry from.
        // Without this a budget created today would open next week with a
        // whole extra allowance, because an empty week looks like an unspent
        // one. Not remembered: on a new device this is also what an
        // unfinished first sync looks like, and the answer changes once it is.
        if (!DBHelper.ExpenseExistsBeforeWeek(budget.UniqueId, 0, budget.StartDay)) {
            return false;
        }

        double amount = amountToCarry(budget.Amount,
                DBHelper.GetExpensesForWeek(budget.UniqueId, 7, budget.StartDay));
        if (amount == 0) {
            Settings.setCarriedWeek(context, budget.UniqueId, thisWeek);
            return false;
        }

        Calendar start = Calendar.getInstance();
        while ((start.get(Calendar.DAY_OF_WEEK) - 1) != budget.StartDay) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }

        Expense e = new Expense();
        e.Amount = amount;
        e.Date = new GregorianCalendar(start.get(Calendar.YEAR), start.get(Calendar.MONTH),
                start.get(Calendar.DAY_OF_MONTH)).getTime();
        e.BudgetId = budget.UniqueId;
        e.Id = Settings.getNextId(context);
        e.Description = context.getString(R.string.carry_balance_expense_description);
        e.IsSystem = true;
        DBHelper.AddExpense(e, DBHelper.CREATED_STATE_KEY);

        Settings.setCarriedWeek(context, budget.UniqueId, thisWeek);
        return true;
    }
}
