package com.andrewovens.weeklybudget2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class AddExpenseWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context,
                                AppWidgetManager appWidgetManager, int appWidgetId) {

        Intent intent = new Intent(context, WeekActivity.class);
        intent.putExtra(WeekActivity.EXTRA_ADD_EXPENSE, true);
        // Without these, tapping the widget while the app is already running
        // just brings the existing task forward and the extra is never
        // delivered, so the add-expense form never opens.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // From Android 12 a PendingIntent must state its mutability explicitly
        // or the framework throws. Nothing fills in extras later, so it is
        // immutable.
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.add_expense_widget);

        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);

        DBHelper.OpenDB(context);

        Budget budget = Settings.getBudget(context);
        if (budget != null) {
            List<Expense> expenses = DBHelper.GetExpensesForWeek(budget.UniqueId, 0, budget.StartDay);
            double total = 0;
            for (int i = 0; i < expenses.size(); i++) {
                total += expenses.get(i).Amount;
            }
            double remaining = budget.Amount - total;
            double rounded = Math.round(remaining * 100) / 100.0;
            views.setTextViewText(R.id.appwidget_amount, Helpers.currencyString(Math.abs(rounded)));
            views.setTextColor(R.id.appwidget_amount, ContextCompat.getColor(context,
                    remaining >= 0 ? R.color.widget_amount : R.color.amount_over_budget));
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
