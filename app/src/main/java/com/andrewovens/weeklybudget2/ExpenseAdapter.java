package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * The week's expenses, grouped under a header per day.
 *
 * <p>The old flat table repeated the weekday in a boxed column on every row,
 * which is where most of its visual noise came from. Grouping says it once and
 * adds the day's total, which is the number you actually want when scanning a
 * week.
 */
final class ExpenseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EXPENSE = 1;
    private static final int TYPE_TOTAL = 2;

    /** Either a day header (label + total) or one expense. */
    private static final class Row {
        final int type;
        final String label;
        final double total;
        final Expense expense;

        Row(String label, double total) {
            this.type = TYPE_HEADER;
            this.label = label;
            this.total = total;
            this.expense = null;
        }

        Row(Expense expense) {
            this.type = TYPE_EXPENSE;
            this.label = null;
            this.total = 0;
            this.expense = expense;
        }

        /** The footer: a pre-formatted string, since it is not a bare amount. */
        private Row(String formatted) {
            this.type = TYPE_TOTAL;
            this.label = formatted;
            this.total = 0;
            this.expense = null;
        }

        static Row total(String formatted) {
            return new Row(formatted);
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private CategoryIndex categories;
    private final ExpenseRow.Actions actions;
    private final boolean allowCopy;

    /**
     * Compact mode: a flat list, with the day on each row instead of a heading
     * over each group.
     *
     * <p>Passed in rather than read from a context here, so that the grouping and
     * the row layout can never disagree: the screen decides once, and toggling the
     * setting recreates it.
     */
    private boolean dense;

    void setDense(boolean dense) {
        this.dense = dense;
    }

    /**
     * The spent-against-budget line shown under the last expense, in both layouts.
     *
     * <p>Set before {@link #setExpenses}.
     */
    void setTotal(String formatted) {
        this.total = formatted;
    }

    private String total;

    ExpenseAdapter(ExpenseRow.Actions actions, boolean allowCopy) {
        this.actions = actions;
        this.allowCopy = allowCopy;
        setHasStableIds(false);
    }

    void setCategories(CategoryIndex categories) {
        this.categories = categories;
    }

    /**
     * {@code expenses} must already be ordered by date, as the DB returns
     * them.
     *
     * <p>The whole week is replaced at once — a sync can change any row, and
     * the list is at most a few dozen items — so this does not try to diff.
     */
    @SuppressLint("NotifyDataSetChanged")
    void setExpenses(List<Expense> expenses) {
        rows.clear();

        if (dense) {
            // Flat and in date order — deliberately a running list of the week
            // rather than a set of days. Several long-time users treat the week
            // that way and do not always file an expense under the right day.
            for (Expense expense : expenses) {
                rows.add(new Row(expense));
            }
            if (total != null && !expenses.isEmpty()) {
                rows.add(Row.total(total));
            }
            notifyDataSetChanged();
            return;
        }

        int i = 0;
        while (i < expenses.size()) {
            String day = Dates.dayKey(expenses.get(i).Date);

            int end = i;
            double dayTotal = 0;
            while (end < expenses.size() && Dates.dayKey(expenses.get(end).Date).equals(day)) {
                dayTotal += expenses.get(end).Amount;
                end++;
            }

            // The header's text needs a Context, so the row carries the raw
            // key and it is formatted at bind time.
            rows.add(new Row(day, dayTotal));

            for (int j = i; j < end; j++) {
                rows.add(new Row(expenses.get(j)));
            }

            i = end;
        }

        if (total != null && !expenses.isEmpty()) {
            rows.add(Row.total(total));
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_day_header, parent, false));
        }
        if (viewType == TYPE_TOTAL) {
            return new TotalHolder(inflater.inflate(R.layout.item_week_total, parent, false));
        }
        return new ExpenseHolder(ExpenseRow.inflate(inflater, parent));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof TotalHolder) {
            ((TotalHolder) holder).value.setText(row.label);
        } else if (holder instanceof HeaderHolder) {
            HeaderHolder h = (HeaderHolder) holder;
            h.label.setText(Dates.formatDayKey(h.label.getContext(), row.label));
            h.total.setText(Helpers.currencyString(row.total));
        } else {
            ExpenseRow.bind(holder.itemView, row.expense, categories,
                    ExpenseRow.Subtitle.CATEGORY, allowCopy, actions);
        }
    }

    private static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView label;
        final TextView total;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.day_header_label);
            total = itemView.findViewById(R.id.day_header_total);
        }
    }

    private static final class TotalHolder extends RecyclerView.ViewHolder {
        final TextView value;

        TotalHolder(@NonNull View itemView) {
            super(itemView);
            value = itemView.findViewById(R.id.week_total_value);
        }
    }

    private static final class ExpenseHolder extends RecyclerView.ViewHolder {
        ExpenseHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
