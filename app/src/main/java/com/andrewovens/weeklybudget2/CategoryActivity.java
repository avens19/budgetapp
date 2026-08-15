package com.andrewovens.weeklybudget2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.util.List;

public class CategoryActivity extends BaseActivity
        implements Navigation.OnDestinationSelected, CategoryAdapter.Actions {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;
    private CategoryAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Navigation.setUp(this, Navigation.CATEGORY, this);

        try {
            Intent i = getIntent();
            String budgetString = i.getStringExtra("budget");
            _budget = Budget.fromJson(new JSONObject(budgetString));
        } catch (Exception e) {
            this.finish();
            e.printStackTrace();
            return;
        }

        _adapter = new CategoryAdapter(this);
        RecyclerView list = findViewById(R.id.category_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(_adapter);

        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(CategoryActivity.this::loadData);
            }
        };
        Sync.registerCompletionReceiver(this, _syncReceiver);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(_syncReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Navigation.select(this, Navigation.CATEGORY);

        loadData();

        this.invalidateOptionsMenu();

        Sync.start(this);
    }

    private void loadData() {
        _budget = Settings.getBudget(this);

        if (_budget == null) {
            this.finish();
            return;
        }

        List<Category> list = DBHelper.GetActiveCategories(_budget.UniqueId, null);
        _adapter.setCategories(list, CategoryIndex.of(this, list));

        View empty = findViewById(R.id.category_manage_empty);
        if (list.isEmpty()) {
            EmptyState.show(empty, R.drawable.ic_label, R.string.category_manage_empty_title,
                    R.string.category_manage_empty_body);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    /** Unsynced local edits must stay "created" so the sync still creates them. */
    private static String pendingStateFor(Category c) {
        return DBHelper.CREATED_STATE_KEY.equals(c.State)
                ? DBHelper.CREATED_STATE_KEY
                : DBHelper.EDITED_STATE_KEY;
    }

    @Override
    public void onRename(final Category c) {
        final View v = View.inflate(this, R.layout.rename_category, null);

        EditText et = v.findViewById(R.id.new_category_name);
        et.setText(c.Name);
        et.setSelection(et.getText().length());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.category_context_rename)
                .setCancelable(true)
                .setView(v)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.rename, (dialog, which) -> {
                    String categoryName = et.getText().toString().trim();

                    if (categoryName.isEmpty()) {
                        Toast.makeText(CategoryActivity.this, R.string.error_category_name_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    c.Name = categoryName;
                    DBHelper.EditCategory(c, pendingStateFor(c));
                    loadData();
                    Sync.start(CategoryActivity.this);
                })
                .show();
    }

    @Override
    public void onDelete(final Category c) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.category_context_delete)
                .setMessage(getString(R.string.category_delete_message, c.Name))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    c.IsDeleted = true;
                    DBHelper.EditCategory(c, pendingStateFor(c));
                    _adapter.remove(c);
                    Sync.start(CategoryActivity.this);
                })
                .show();
    }

    @Override
    public void onDestinationSelected(int position) {
        ScreenSwitcher.goToPosition(this, position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.month, menu);
        if (_budget != null) {
            MenuItem s = menu.findItem(R.id.action_current_budget);
            s.setTitle(getString(R.string.current_budget_named, _budget.Name));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ScreenSwitcher.onOptionsItemSelected(this, item, _budget)
                || super.onOptionsItemSelected(item);
    }
}
