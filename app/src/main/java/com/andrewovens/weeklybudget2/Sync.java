package com.andrewovens.weeklybudget2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pushes local changes to the API and pulls back anything new.
 *
 * <p>This used to be an {@link android.app.IntentService}. Android 8 forbids
 * starting a background service from the background, so every sync kicked off
 * from a widget update or a broadcast was silently dropped by the
 * {@code catch} around {@code startService}. The work is a short-lived network
 * plus database round trip that nobody needs to outlive the process, so it now
 * runs on a private single-threaded executor instead — no service, no
 * background-start restriction, and no notification obligation.
 */
final class Sync {

    /** Broadcast sent, package-internal, once a sync attempt finishes. */
    static final String SYNC_COMPLETE = "com.andrewovens.weeklybudget2.SYNC_COMPLETE";

    /**
     * Syncs are all identical, so queueing more than one behind the running one
     * is pointless. Anything beyond that is dropped.
     */
    private static final int MAX_QUEUED = 2;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "budget-sync");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicInteger QUEUED = new AtomicInteger();

    private Sync() {
    }

    /** Registers {@code receiver} for {@link #SYNC_COMPLETE} on this app only. */
    static void registerCompletionReceiver(Context context, BroadcastReceiver receiver) {
        ContextCompat.registerReceiver(context, receiver, new IntentFilter(SYNC_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    static void start(Context context) {
        final Context appContext = context.getApplicationContext();

        if (QUEUED.incrementAndGet() > MAX_QUEUED) {
            QUEUED.decrementAndGet();
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                run(appContext);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // A failed sync is not fatal: everything stays queued locally
                // and the next sync will retry it.
            } finally {
                QUEUED.decrementAndGet();
                Intent done = new Intent(SYNC_COMPLETE).setPackage(appContext.getPackageName());
                appContext.sendBroadcast(done);
            }
        });
    }

    private static void run(Context context) throws Exception {
        DBHelper.OpenDB(context);

        Budget budget = Settings.getBudget(context);
        if (budget == null) {
            return;
        }

        String watermark = budget.Watermark;

        budget = Budget.update(budget, API.GetBudget(budget.UniqueId));

        List<Category> categories = new ArrayList<>();
        categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.CREATED_STATE_KEY));
        categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.EDITED_STATE_KEY));
        categories.addAll(DBHelper.GetUnsyncedCategories(budget.UniqueId, DBHelper.DELETED_STATE_KEY));

        List<Expense> expenses = new ArrayList<>();
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
        Budget.updateStoredBudget(context, budget);

        for (Category c : newCategories) {
            DBHelper.AddCategory(c, DBHelper.SYNCED_STATE_KEY);
        }

        for (Expense e : newExpenses) {
            if (!e.IsDeleted)
                DBHelper.AddExpense(e, DBHelper.SYNCED_STATE_KEY);
            else
                DBHelper.DeleteExpense(e);
        }
    }

    private static String UTCTimeString() {
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormatGmt.format(new Date());
    }
}
