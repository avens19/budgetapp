package com.andrewovens.weeklybudget2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class AddExpenseWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
		}
	}

	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}

	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}

	static void updateAppWidget(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId) {
		
		// Create an Intent to launch ExampleActivity
        Intent intent = new Intent(context, WeekActivity.class);
        intent.putExtra("ADD", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.add_expense_widget);
		
		views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);

		DBHelper.OpenDB(context);
		DBHelper.CreateExpensesTable();

		Budget _budget = Settings.getBudget(context);
		if(_budget != null) {
			List<Expense> expenses = DBHelper.GetExpensesForWeek(_budget.UniqueId, 0, _budget.StartDay);
			double total = 0;
			for (int i = 0; i < expenses.size(); i++) {
				total += expenses.get(i).Amount;
			}
			double remaining = _budget.Amount - total;
			double rounded = Math.round(remaining*100)/100.0;
			views.setTextViewText(R.id.appwidget_amount, Helpers.currencyString(Math.abs(rounded)));
			views.setTextColor(R.id.appwidget_amount, remaining >= 0 ? Color.BLACK : Color.RED);
		}
		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}
