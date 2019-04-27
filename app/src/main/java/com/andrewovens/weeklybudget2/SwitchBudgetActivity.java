package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SwitchBudgetActivity extends Activity {

    private BudgetAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_budget);

        Budget[] budgets = Settings.getBudgets(this);

        ListView lv = findViewById(R.id.switch_list);

        ArrayList<Budget> bs = new ArrayList<>();
        Collections.addAll(bs, budgets);

        _adapter = new BudgetAdapter(this, R.layout.budget_row, bs);

        lv.setAdapter(_adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Budget b = _adapter.getItem(i);
                try {
                    Settings.setBudget(SwitchBudgetActivity.this, b);
                    SwitchBudgetActivity.this.setResult(Activity.RESULT_OK);
                    SwitchBudgetActivity.this.finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Helpers.showNetworkErrorToastOnUi(SwitchBudgetActivity.this, R.string.error_network);
                }
            }
        });

        registerForContextMenu(lv);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ListView lv = (ListView) v;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        lv.setTag(lv.getAdapter().getItem(info.position));
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.switch_budget_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListView lv = findViewById(R.id.switch_list);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Budget b = (Budget) lv.getItemAtPosition(info.position);

        try {
            if (item.getItemId() == R.id.remove_budget) {
                if (_adapter.getLength() <= 1) {
                    Settings.setBudget(this, null);
                    Settings.setBudgets(this, null);
                    SwitchBudgetActivity.this.setResult(Activity.RESULT_OK);
                    SwitchBudgetActivity.this.finish();
                    return true;
                } else {
                    _adapter.removeBudget(b);
                    Settings.setBudgets(this, _adapter.getBudgets());
                    Budget currentBudget = Settings.getBudget(this);
                    if (currentBudget != null && b.UniqueId.equals(currentBudget.UniqueId)) {
                        Settings.setBudget(this, _adapter.getItem(0));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    public void newBudgetOnClick(View view) {
        Intent i = new Intent(this, NewBudgetActivity.class);
        startActivityForResult(i, 1);
    }

    public void joinBudgetOnClick(View view) {
        Intent i = new Intent(this, JoinBudgetActivity.class);
        startActivityForResult(i, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK)
            this.finish();
    }

    public class BudgetAdapter extends ArrayAdapter<Budget> {
        private final Context context;
        private final int resourceID;
        private final List<Budget> _budgets;

        BudgetAdapter(Context context, int resource, ArrayList<Budget> budgets) {
            super(context, resource, budgets);

            this.context = context;
            this.resourceID = resource;
            this._budgets = budgets;
        }

        int getLength() {
            return _budgets.size();
        }

        public Budget getItem(int index) {
            return _budgets.get(index);
        }

        Budget[] getBudgets() {
            Budget[] array = new Budget[_budgets.size()];
            _budgets.toArray(array);
            return array;
        }

        void removeBudget(Budget b) {
            int index = -1;
            for (int i = 0; i < _budgets.size(); i++) {
                if (_budgets.get(i).UniqueId.equals(b.UniqueId)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                _budgets.remove(index);
                this.notifyDataSetChanged();
            }
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = convertView != null ? convertView : inflater.inflate(resourceID, parent, false);

            Budget c = this.getItem(position);

            TextView name = rowView.findViewById(R.id.budget_row_name);
            assert c != null;
            name.setText(c.Name);

            return rowView;
        }

    }
}
