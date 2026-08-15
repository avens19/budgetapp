package com.andrewovens.weeklybudget2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
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

        API.Page<Category> incomingCategories = API.GetCategories(budget.UniqueId, watermark);
        API.Page<Expense> incomingExpenses = API.GetExpenses(budget.UniqueId, watermark);

        // Apply first, advance the watermark second. A crash in between costs
        // one redundant re-fetch; the other order would lose the changes.
        for (Category c : incomingCategories.items) {
            DBHelper.AddCategory(c, DBHelper.SYNCED_STATE_KEY);
        }

        for (Expense e : incomingExpenses.items) {
            if (!e.IsDeleted)
                DBHelper.AddExpense(e, DBHelper.SYNCED_STATE_KEY);
            else
                DBHelper.DeleteExpense(e);
        }

        String next = nextWatermark(incomingCategories.watermark, incomingExpenses.watermark);
        if (next != null) {
            budget.Watermark = next;
            Budget.updateStoredBudget(context, budget);
        }
    }

    /**
     * The point both change feeds are complete up to.
     *
     * <p>This used to be the device's own clock, read just before the two
     * GETs. The server compares the watermark against timestamps it stamped
     * from <em>its</em> clock, so any skew between the two was silent data
     * loss: a device running even a minute fast would store a watermark ahead
     * of the server's real time, and anything another device wrote inside that
     * minute was never handed over again. Taking the server's own answer for
     * "now" removes the device clock from the sync entirely.
     *
     * <p>The two feeds are fetched in separate requests, so their watermarks
     * are separate instants. One value is stored for both, and it has to be
     * the <em>earlier</em>: the later one would skip anything written to the
     * other collection between the two requests. The earlier one can only
     * cause a small overlap on the next sync, and re-applying a row that is
     * already stored is a no-op.
     *
     * <p>Null means the server sent no header at all — an older deployment, or
     * a proxy that stripped it. The caller then leaves the stored watermark
     * alone: syncing the same window twice is wasteful but correct, and it is
     * the only answer available that is not a guess.
     */
    @Nullable
    static String nextWatermark(@Nullable String a, @Nullable String b) {
        if (a == null || b == null) {
            return null;
        }
        // Both come from the same server in the same fixed-width UTC format
        // ("yyyy-MM-ddTHH:mm:ss.fffffffZ"), so ordering them as text orders
        // them chronologically.
        return a.compareTo(b) <= 0 ? a : b;
    }
}
