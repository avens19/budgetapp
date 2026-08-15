package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A month, as one card per week.
 *
 * <p>The screen used to draw a seven-column grid of day numbers with the
 * week's total in an eighth column. At phone widths the numbers were a few
 * pixels tall and carried no information the calendar app does not already
 * give you; the comparison that matters is each week's spend against the
 * weekly budget, which the bar shows directly.
 */
final class WeekSummaryAdapter extends RecyclerView.Adapter<WeekSummaryAdapter.Holder> {

    interface OnWeekSelected {
        void onWeekSelected(DateTotal week);
    }

    private final List<DateTotal> weeks = new ArrayList<>();
    private final OnWeekSelected listener;
    private double weeklyBudget;

    WeekSummaryAdapter(OnWeekSelected listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    void setWeeks(List<DateTotal> newWeeks, double weeklyBudget) {
        this.weeklyBudget = weeklyBudget;
        weeks.clear();
        weeks.addAll(newWeeks);
        notifyDataSetChanged();
    }

    DateTotal get(int position) {
        return weeks.get(position);
    }

    @Override
    public int getItemCount() {
        return weeks.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_summary, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final DateTotal week = weeks.get(position);
        Context context = holder.itemView.getContext();

        Calendar end = (Calendar) week.Date.clone();
        end.add(Calendar.DAY_OF_YEAR, 6);
        holder.range.setText(context.getString(R.string.month_week_range,
                Dates.getShortDateString(context, week.Date.getTime()),
                Dates.getShortDateString(context, end.getTime())));

        holder.total.setText(Helpers.currencyString(week.Total));

        boolean over = weeklyBudget > 0 && week.Total > weeklyBudget;
        int amountColor = ContextCompat.getColor(context,
                over ? R.color.amount_over_budget : R.color.amount_within_budget);
        holder.total.setTextColor(amountColor);

        int progress = weeklyBudget > 0
                ? (int) Math.round(Math.min(week.Total / weeklyBudget, 1.0) * 100)
                : 0;
        holder.progress.setProgressCompat(Math.max(progress, 0), false);
        holder.progress.setIndicatorColor(over
                ? ContextCompat.getColor(context, R.color.amount_over_budget)
                : MaterialColors.getColor(holder.itemView, androidx.appcompat.R.attr.colorPrimary));

        if (weeklyBudget <= 0) {
            holder.state.setText(null);
        } else if (over) {
            holder.state.setText(context.getString(R.string.month_over_by,
                    Helpers.currencyString(week.Total - weeklyBudget)));
        } else {
            holder.state.setText(context.getString(R.string.month_under_by,
                    Helpers.currencyString(weeklyBudget - week.Total)));
        }

        holder.itemView.setOnClickListener(v -> listener.onWeekSelected(week));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView range;
        final TextView total;
        final TextView state;
        final LinearProgressIndicator progress;

        Holder(@NonNull View itemView) {
            super(itemView);
            range = itemView.findViewById(R.id.week_summary_range);
            total = itemView.findViewById(R.id.week_summary_total);
            state = itemView.findViewById(R.id.week_summary_state);
            progress = itemView.findViewById(R.id.week_summary_progress);
        }
    }
}
