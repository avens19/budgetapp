package com.andrewovens.weeklybudget2;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Binds one expense into {@code item_expense}.
 *
 * <p>Two screens show expense rows in a {@link androidx.recyclerview.widget.RecyclerView}
 * and two show a handful of them in a plain {@code LinearLayout}, so the
 * binding lives here rather than inside an adapter.
 *
 * <p>The row's actions used to be a long-press context menu, which is
 * undiscoverable. They are now a visible overflow button, and the row itself
 * opens the edit form.
 */
final class ExpenseRow {

    /**
     * What the second line of a row says. Under a day header the date is
     * already known and the category is the useful fact; in a category
     * drill-down it is the other way round, and repeating the category name
     * under a heading of the same name says nothing.
     */
    enum Subtitle {
        CATEGORY,
        DATE
    }

    interface Actions {
        void onEdit(Expense expense);

        void onDelete(Expense expense);

        /** Only offered when {@code allowCopy} is set on the bind call. */
        void onCopyToNextWeek(Expense expense);
    }

    private ExpenseRow() {
    }

    static View inflate(LayoutInflater inflater, ViewGroup parent) {
        // The compact layout is a display preference, so it is read here rather
        // than threaded through every caller: all four places that show expense
        // rows want to honour it, and none of them want to think about it.
        int layout = Settings.isDenseLayout(parent.getContext())
                ? R.layout.item_expense_dense
                : R.layout.item_expense;
        return inflater.inflate(layout, parent, false);
    }

    static void bind(@NonNull View row, @NonNull final Expense expense,
                     @NonNull CategoryIndex categories, @NonNull Subtitle subtitle,
                     final boolean allowCopy, @NonNull final Actions actions) {
        Context context = row.getContext();

        TextView description = row.findViewById(R.id.expense_description);
        description.setText(expense.Description);

        TextView category = row.findViewById(R.id.expense_category);
        if (Settings.isDenseLayout(context)) {
            // Dense mode drops the day headings, so the row has to say when — in as
            // little space as will do the job. Inside a single week a weekday is
            // unambiguous; a category drill-down can span a month, where "Mon" could
            // be any of four, so that gets the date. The category itself is still
            // there as the colour of the dot.
            category.setText(subtitle == Subtitle.DATE
                    ? Dates.getShortDateString(context, expense.Date)
                    : Dates.getWeekDay(expense.Date));
        } else {
            category.setText(subtitle == Subtitle.DATE
                    ? Dates.getFullDateString(context, expense.Date)
                    : categories.nameFor(expense.CategoryId));
        }

        TextView amount = row.findViewById(R.id.expense_amount);
        amount.setText(Helpers.currencyString(expense.Amount));

        ImageView dot = row.findViewById(R.id.expense_dot);
        ImageViewCompat.setImageTintList(dot,
                ColorStateList.valueOf(categories.colorFor(expense.CategoryId)));

        // The dense layout wraps its content so a divider can sit outside the
        // touch target; the roomy one is its own target.
        View touch = row.findViewById(R.id.expense_touch);
        (touch != null ? touch : row).setOnClickListener(v -> actions.onEdit(expense));

        MaterialButton menu = row.findViewById(R.id.expense_menu);
        menu.setContentDescription(context.getString(R.string.expense_options, expense.Description));
        menu.setOnClickListener(v -> showMenu(v, expense, allowCopy, actions));
    }

    private static void showMenu(View anchor, final Expense expense, boolean allowCopy,
                                 final Actions actions) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.inflate(allowCopy ? R.menu.expense_row : R.menu.expense_row_no_copy);
        popup.setForceShowIcon(true);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.context_edit) {
                actions.onEdit(expense);
            } else if (id == R.id.context_delete) {
                actions.onDelete(expense);
            } else if (id == R.id.context_copy) {
                actions.onCopyToNextWeek(expense);
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    /** Fills {@code container} with rows for {@code expenses}, reusing children. */
    static void fill(@NonNull ViewGroup container, @NonNull List<Expense> expenses,
                     @NonNull CategoryIndex categories, @NonNull Subtitle subtitle,
                     boolean allowCopy, @NonNull Actions actions) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        while (container.getChildCount() > expenses.size()) {
            container.removeViewAt(container.getChildCount() - 1);
        }
        while (container.getChildCount() < expenses.size()) {
            container.addView(inflate(inflater, container));
        }

        for (int i = 0; i < expenses.size(); i++) {
            bind(container.getChildAt(i), expenses.get(i), categories, subtitle, allowCopy, actions);
        }
    }
}
