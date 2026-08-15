package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SwitchBudgetActivity extends BaseActivity {

    private static final int CREATE_OR_JOIN = 1;

    private BudgetAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_budget);

        Budget[] budgets = Settings.getBudgets(this);

        ArrayList<Budget> bs = new ArrayList<>();
        if (budgets != null) {
            // Null until the one-off migration in WeekActivity has run, which
            // happens on a background thread — reaching this screen first used
            // to crash here.
            Collections.addAll(bs, budgets);
        } else {
            Budget current = Settings.getBudget(this);
            if (current != null) {
                bs.add(current);
            }
        }

        Budget current = Settings.getBudget(this);
        _adapter = new BudgetAdapter(bs, current != null ? current.UniqueId : null);

        RecyclerView list = findViewById(R.id.switch_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(_adapter);

        findViewById(R.id.switch_add_budget).setOnClickListener(v ->
                startActivityForResult(new Intent(this, NewBudgetActivity.class), CREATE_OR_JOIN));
        findViewById(R.id.switch_join_budget).setOnClickListener(v ->
                startActivityForResult(new Intent(this, JoinBudgetActivity.class), CREATE_OR_JOIN));
    }

    private void select(Budget b) {
        try {
            Settings.setBudget(this, b);
            setResult(Activity.RESULT_OK);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
            Helpers.showNetworkErrorToastOnUi(this, R.string.error_network);
        }
    }

    private void remove(Budget b) {
        try {
            if (_adapter.getItemCount() <= 1) {
                Settings.setBudget(this, null);
                Settings.setBudgets(this, null);
                setResult(Activity.RESULT_OK);
                finish();
                return;
            }

            _adapter.removeBudget(b);
            Settings.setBudgets(this, _adapter.getBudgets());

            Budget currentBudget = Settings.getBudget(this);
            if (currentBudget != null && b.UniqueId.equals(currentBudget.UniqueId)) {
                Budget replacement = _adapter.getItem(0);
                Settings.setBudget(this, replacement);
                _adapter.setCurrent(replacement.UniqueId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            this.finish();
        }
    }

    /** The budgets this device knows about, with the active one ticked. */
    private final class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.Holder> {

        private final List<Budget> _budgets;
        private String _currentId;

        BudgetAdapter(List<Budget> budgets, String currentId) {
            _budgets = budgets;
            _currentId = currentId;
        }

        Budget getItem(int index) {
            return _budgets.get(index);
        }

        Budget[] getBudgets() {
            return _budgets.toArray(new Budget[0]);
        }

        @SuppressLint("NotifyDataSetChanged")
        void setCurrent(String uniqueId) {
            _currentId = uniqueId;
            notifyDataSetChanged();
        }

        void removeBudget(Budget b) {
            for (int i = 0; i < _budgets.size(); i++) {
                if (_budgets.get(i).UniqueId.equals(b.UniqueId)) {
                    _budgets.remove(i);
                    notifyItemRemoved(i);
                    return;
                }
            }
        }

        @Override
        public int getItemCount() {
            return _budgets.size();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_budget, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            final Budget b = _budgets.get(position);

            holder.name.setText(b.Name);
            holder.amount.setText(getString(R.string.budget_weekly_amount,
                    Helpers.currencyString(b.Amount)));
            holder.check.setVisibility(b.UniqueId.equals(_currentId) ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> select(b));
            holder.menu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.switch_budget_context);
                popup.setForceShowIcon(true);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.remove_budget) {
                        remove(b);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        final class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView amount;
            final View check;
            final MaterialButton menu;

            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.budget_row_name);
                amount = itemView.findViewById(R.id.budget_row_amount);
                check = itemView.findViewById(R.id.budget_row_check);
                menu = itemView.findViewById(R.id.budget_row_menu);
            }
        }
    }
}
