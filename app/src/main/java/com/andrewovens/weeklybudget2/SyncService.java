package com.andrewovens.weeklybudget2;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
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

    public static final String SYNCCOMPLETE = "SYNCCOMPLETE";

    // The IntentService class will only ever run one intent at a time. Since our syncs are all the same
    // it is redundant to run a bunch in a row. Instead, we will track the number of running + queued intents
    // and we will ignore all additional intents if this value is >= 2
    private int queuedIntents = 0;

    public SyncService() {
        super("SyncService");
    }

    public static void startSync(Context c)
    {
        Intent intent = new Intent(c, SyncService.class);
        c.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(queuedIntents < 2) {
            queuedIntents++;
            return super.onStartCommand(intent, flags, startId);
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Budget budget = Settings.getBudget(this);

            if(budget == null){
                return;
            }

            String watermark = budget.Watermark;

            budget = Budget.update(budget, API.GetBudget(budget.UniqueId));

            ArrayList<Category> categories = new ArrayList<Category>();

            categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.CREATEDSTATEKEY));
            categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.EDITEDSTATEKEY));
            categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.DELETEDSTATEKEY));

            ArrayList<Expense> expenses = new ArrayList<Expense>();

            expenses.addAll(DBHelper.GetUnsyncedExpenses(budget.UniqueId, DBHelper.CREATEDSTATEKEY));
            expenses.addAll(DBHelper.GetUnsyncedExpenses(budget.UniqueId, DBHelper.EDITEDSTATEKEY));
            expenses.addAll(DBHelper.GetUnsyncedExpenses(budget.UniqueId, DBHelper.DELETEDSTATEKEY));

            for (Category c : categories) {
                if (c.State.equals(DBHelper.CREATEDSTATEKEY)) {
                    Category category = API.AddCategory(c);
                    DBHelper.ReplaceCategory(c, category, DBHelper.SYNCEDSTATEKEY);
                } else if (c.State.equals(DBHelper.EDITEDSTATEKEY)) {
                    API.EditCategory(c);
                    DBHelper.EditCategory(c, DBHelper.SYNCEDSTATEKEY);
                } else if (c.State.equals(DBHelper.DELETEDSTATEKEY)) {
                    Category category = API.DeleteCategory(c);
                    DBHelper.EditCategory(category, DBHelper.SYNCEDSTATEKEY);
                }
            }

            for (Expense e : expenses) {
                if (e.State.equals(DBHelper.CREATEDSTATEKEY)) {
                    Expense expense = API.AddExpense(e);
                    DBHelper.DeleteExpense(e);
                    DBHelper.AddExpense(expense, DBHelper.SYNCEDSTATEKEY);
                } else if (e.State.equals(DBHelper.EDITEDSTATEKEY)) {
                    API.EditExpense(e);
                    DBHelper.EditExpense(e, DBHelper.SYNCEDSTATEKEY);
                } else if (e.State.equals(DBHelper.DELETEDSTATEKEY)) {
                    API.DeleteExpense(e);
                    DBHelper.DeleteExpense(e);
                }
            }

            String d = UTCTimeString();
            List<Category> newCategories = API.GetCategories(budget.UniqueId, watermark);
            List<Expense> newExpenses = API.GetExpenses(budget.UniqueId, watermark);
            budget.Watermark = d;
            Budget.updateStoredBudget(this, budget);

            for (Category c : newCategories) {
                DBHelper.AddCategory(c, DBHelper.SYNCEDSTATEKEY);
            }

            for (Expense e : newExpenses) {
                if (!e.IsDeleted)
                    DBHelper.AddExpense(e, DBHelper.SYNCEDSTATEKEY);
                else
                    DBHelper.DeleteExpense(e);
            }
        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            Intent i = new Intent(SYNCCOMPLETE);
            sendBroadcast(i);
            queuedIntents--;
        }
    }

    private static String UTCTimeString()
    {
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormatGmt.format(new Date());
    }
}
