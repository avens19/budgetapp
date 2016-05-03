package com.andrewovens.weeklybudget2;

import android.content.Context;
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

    public WeekRowAdapter(Context context, int resource, List<Expense> bah) {
        super(context, resource, bah);

        this.context = context;
        this.resourceID = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView != null ? convertView : inflater.inflate(resourceID, parent, false);

        TextView day = (TextView)rowView.findViewById(R.id.week_row_day);
        day.setText(Dates.getWeekDay(this.getItem(position).Date));

        TextView name = (TextView)rowView.findViewById(R.id.week_row_name);
        name.setText(this.getItem(position).Description);

        TextView amount = (TextView)rowView.findViewById(R.id.week_row_amount);
        amount.setText(Helpers.currencyString(this.getItem(position).Amount));

        return rowView;
    }
}
