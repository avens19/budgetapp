package com.andrewovens.weeklybudget2;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import org.json.JSONObject;

import java.util.List;

public class CategoryActivity extends BaseActivity implements ActionBar.OnNavigationListener {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Navigation.setUp(this, this, Navigation.CATEGORY);

        setUpOnLongClick();

        try {
            Intent i = getIntent();
            String budgetString = i.getStringExtra("budget");
            _budget = Budget.fromJson(new JSONObject(budgetString));
        } catch (Exception e) {
            this.finish();
            e.printStackTrace();
        }

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

        ListView lv = findViewById(R.id.category_list);

        if (lv.getAdapter() == null) {
            CategoryAdapter a = new CategoryAdapter(this, R.layout.category_row, list);
            lv.setAdapter(a);
        } else {
            CategoryAdapter a = (CategoryAdapter) lv.getAdapter();
            a.clear();
            a.addAll(list);
        }
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        return ScreenSwitcher.onNavigationItemSelected(this, Navigation.CATEGORY, position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
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

    private void setUpOnLongClick() {
        ListView lv = this.findViewById(R.id.category_list);
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
        inflater.inflate(R.menu.category_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListView lv = findViewById(R.id.category_list);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Category c = (Category) lv.getItemAtPosition(info.position);

        try {
            int id = item.getItemId();
            if (id == R.id.category_context_rename) {
                showRenameDialog(c);
            } else if (id == R.id.category_context_delete) {
                showDeleteDialog(c);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    /** Unsynced local edits must stay "created" so the sync still creates them. */
    private static String pendingStateFor(Category c) {
        return DBHelper.CREATED_STATE_KEY.equals(c.State)
                ? DBHelper.CREATED_STATE_KEY
                : DBHelper.EDITED_STATE_KEY;
    }

    private void showRenameDialog(final Category c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View v = View.inflate(this, R.layout.rename_category, null);

        EditText et = v.findViewById(R.id.new_category_name);
        et.setText(c.Name);

        builder
                .setTitle(getString(R.string.category_context_rename))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.ok, (dialog, which) -> {
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
                .setView(v);

        Dialog d = builder.create();

        d.show();
    }

    private void showDeleteDialog(final Category c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder
                .setTitle(getString(R.string.category_context_delete))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    c.IsDeleted = true;

                    DBHelper.EditCategory(c, pendingStateFor(c));

                    ListView lv = findViewById(R.id.category_list);

                    ((CategoryAdapter) lv.getAdapter()).remove(c);

                    Sync.start(CategoryActivity.this);
                });

        Dialog d = builder.create();

        d.show();
    }

}
