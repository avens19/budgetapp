package com.andrewovens.weeklybudget2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by andrew on 02/05/16.
 */
public class WeekRowAdapter extends ArrayAdapter<Expense> {
    private final Context context;
    private final int resourceID;

    WeekRowAdapter(Context context, int resource, List<Expense> bah) {
        super(context, resource, bah);

        this.context = context;
        this.resourceID = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView != null ? convertView : inflater.inflate(resourceID, parent, false);
        Expense e = this.getItem(position);

        assert e != null;

        TextView day = rowView.findViewById(R.id.week_row_day);
        day.setText(context.getString(R.string.two_string_newline, Dates.getWeekDay(e.Date), Dates.getDayOfMonth(e.Date)));

        TextView name = rowView.findViewById(R.id.week_row_name);
        name.setText(e.Description);

        TextView amount = rowView.findViewById(R.id.week_row_amount);
        amount.setText(Helpers.currencyString(e.Amount));

        return rowView;
    }
}
