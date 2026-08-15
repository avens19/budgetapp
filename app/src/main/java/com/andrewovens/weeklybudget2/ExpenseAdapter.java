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
    }

    private final List<Row> rows = new ArrayList<>();
    private CategoryIndex categories;
    private final ExpenseRow.Actions actions;
    private final boolean allowCopy;

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
        return new ExpenseHolder(ExpenseRow.inflate(inflater, parent));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
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

    private static final class ExpenseHolder extends RecyclerView.ViewHolder {
        ExpenseHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
