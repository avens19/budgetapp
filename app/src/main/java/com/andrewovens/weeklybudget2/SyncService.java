package com.andrewovens.weeklybudget2;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by andrew on 07/05/16.
 */
public class SyncService extends IntentService {

    public static final String SYNC_COMPLETE = "SYNCCOMPLETE";

    // The IntentService class will only ever run one intent at a time. Since our syncs are all the same
    // it is redundant to run a bunch in a row. Instead, we will track the number of running + queued intents
    // and we will ignore all additional intents if this value is >= 2
    private int queuedIntents = 0;

    public SyncService() {
        super("SyncService");
    }

    public static void startSync(Context c) {
        Intent intent = new Intent(c, SyncService.class);
        try {
            c.startService(intent);
        } catch (Exception ignore) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (queuedIntents < 2) {
            queuedIntents++;
            return super.onStartCommand(intent, flags, startId);
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Budget budget = Settings.getBudget(this);

            if (budget == null) {
                return;
            }

            String watermark = budget.Watermark;

            budget = Budget.update(budget, API.GetBudget(budget.UniqueId));

            ArrayList<Category> categories = new ArrayList<>();

            categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.CREATED_STATE_KEY));
            categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.EDITED_STATE_KEY));
            categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.DELETED_STATE_KEY));

            ArrayList<Expense> expenses = new ArrayList<>();

            expenses.addAll(DBHelper.GetUnsyncedExpenses(budget.UniqueId, DBHelper.CREATED_STATE_KEY));
            expenses.addAll(DBHelper.GetUnsyncedExpenses(budget.UniqueId, DBHelper.EDITED_STATE_KEY));
            expenses.addAll(DBHelper.GetUnsyncedExpenses(budget.UniqueId, DBHelper.DELETED_STATE_KEY));

            for (Category c : categories) {
                switch (c.State) {
                    case DBHelper.CREATED_STATE_KEY: {
                        Category category = API.AddCategory(c);
                        DBHelper.ReplaceCategory(c, category);
                        break;
                    }
                    case DBHelper.EDITED_STATE_KEY:
                        API.EditCategory(c);
                        DBHelper.EditCategory(c, DBHelper.SYNCED_STATE_KEY);
                        break;
                    case DBHelper.DELETED_STATE_KEY: {
                        Category category = API.DeleteCategory(c);
                        DBHelper.EditCategory(category, DBHelper.SYNCED_STATE_KEY);
                        break;
                    }
                }
            }

            for (Expense e : expenses) {
                switch (e.State) {
                    case DBHelper.CREATED_STATE_KEY:
                        Expense expense = API.AddExpense(e);
                        DBHelper.ReplaceExpense(e, expense);
                        break;
                    case DBHelper.EDITED_STATE_KEY:
                        API.EditExpense(e);
                        DBHelper.EditExpense(e, DBHelper.SYNCED_STATE_KEY);
                        break;
                    case DBHelper.DELETED_STATE_KEY:
                        API.DeleteExpense(e);
                        DBHelper.DeleteExpense(e);
                        break;
                }
            }

            String d = UTCTimeString();
            List<Category> newCategories = API.GetCategories(budget.UniqueId, watermark);
            List<Expense> newExpenses = API.GetExpenses(budget.UniqueId, watermark);
            budget.Watermark = d;
            Budget.updateStoredBudget(this, budget);

            for (Category c : newCategories) {
                DBHelper.AddCategory(c, DBHelper.SYNCED_STATE_KEY);
            }

            for (Expense e : newExpenses) {
                if (!e.IsDeleted)
                    DBHelper.AddExpense(e, DBHelper.SYNCED_STATE_KEY);
                else
                    DBHelper.DeleteExpense(e);
            }
        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        } finally {
            Intent i = new Intent(SYNC_COMPLETE);
            sendBroadcast(i);
            queuedIntents--;
        }
    }

    private static String UTCTimeString() {
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormatGmt.format(new Date());
    }
}
