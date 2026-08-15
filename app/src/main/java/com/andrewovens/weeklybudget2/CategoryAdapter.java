package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** The manage-categories list: rename or delete, from a visible row menu. */
final class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {

    interface Actions {
        void onRename(Category category);

        void onDelete(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private CategoryIndex index;
    private final Actions actions;

    CategoryAdapter(Actions actions) {
        this.actions = actions;
    }

    @SuppressLint("NotifyDataSetChanged")
    void setCategories(List<Category> newCategories, CategoryIndex index) {
        this.index = index;
        categories.clear();
        categories.addAll(newCategories);
        notifyDataSetChanged();
    }

    void remove(Category category) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).Id == category.Id) {
                categories.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final Category category = categories.get(position);

        holder.name.setText(category.Name);
        ImageViewCompat.setImageTintList(holder.dot,
                ColorStateList.valueOf(index.colorFor(category.Id)));

        holder.menu.setContentDescription(holder.itemView.getContext()
                .getString(R.string.category_more_options, category.Name));

        View.OnClickListener open = v -> showMenu(holder.menu, category);
        holder.menu.setOnClickListener(open);
        holder.itemView.setOnClickListener(v -> actions.onRename(category));
    }

    private void showMenu(View anchor, final Category category) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.inflate(R.menu.category_row_menu);
        popup.setForceShowIcon(true);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.category_context_rename) {
                actions.onRename(category);
            } else if (id == R.id.category_context_delete) {
                actions.onDelete(category);
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final ImageView dot;
        final MaterialButton menu;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.category_row_name);
            dot = itemView.findViewById(R.id.category_dot);
            menu = itemView.findViewById(R.id.category_menu);
        }
    }
}
