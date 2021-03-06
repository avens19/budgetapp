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
public class CategoryAdapter extends ArrayAdapter<Category> {
    private final Context context;
    private final int resourceID;

    CategoryAdapter(Context context, int resource, List<Category> cats) {
        super(context, resource, cats);

        this.context = context;
        this.resourceID = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView != null ? convertView : inflater.inflate(resourceID, parent, false);

        Category c = this.getItem(position);

        TextView name = rowView.findViewById(R.id.category_row_name);
        assert c != null;
        name.setText(c.Name);

        return rowView;
    }

}
