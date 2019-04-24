package com.andrewovens.weeklybudget2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;

public class CategoryActivity extends Activity implements ActionBar.OnNavigationListener {

    private Budget _budget;
    private BroadcastReceiver _syncReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Set up the action bar to show a dropdown list.
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<>(
                        actionBar.getThemedContext(),
                        R.layout.main_menu_item,
                        R.id.main_menu_item_text,
                        new String[]{
                                getString(R.string.title_week),
                                getString(R.string.title_month),
                                getString(R.string.title_category_week),
                                getString(R.string.title_category_month),
                                getString(R.string.title_category),
                        }),
                this);

        setUpOnLongClick();

        try {
            Intent i = getIntent();
            String budgetString = i.getStringExtra("budget");
            _budget = Budget.fromJson(new JSONObject(budgetString));
        } catch (Exception e) {
            this.finish();
            e.printStackTrace();
        }

        IntentFilter syncFilter = new IntentFilter(SyncService.SYNC_COMPLETE);
        _syncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadData();
                    }
                });
            }
        };
        registerReceiver(_syncReceiver, syncFilter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(_syncReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setSelectedNavigationItem(4);

        loadData();

        this.invalidateOptionsMenu();

        SyncService.startSync(this);
    }

    private void loadData() {
        _budget = Settings.getBudget(this);


        assert _budget != null;
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
        if (position == 0) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_WEEK);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 1) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_MONTH);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 2) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_CATEGORY_WEEK);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 3) {
            try {
                Intent i = new Intent();
                i.putExtra(WeekActivity.GOTO_ACTIVITY, WeekActivity.GOTO_CATEGORY_MONTH);
                this.setResult(Activity.RESULT_OK, i);
                this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.month, menu);
        if (_budget != null) {
            MenuItem s = menu.findItem(R.id.action_current_budget);
            s.setTitle(this.getString(R.string.current_budget) + " " + _budget.Name);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_current_budget) {
            if (_budget != null) {
                try {
                    Intent i = new Intent(this, SwitchBudgetActivity.class);
                    int SWITCH_BUDGET = 2;
                    startActivityForResult(i, SWITCH_BUDGET);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        if (id == R.id.action_settings) {
            if (_budget != null) {
                try {
                    Intent i = new Intent(this, NewBudgetActivity.class);
                    i.putExtra("budget", _budget.toJson(false).toString());
                    int EDIT_BUDGET = 1;
                    startActivityForResult(i, EDIT_BUDGET);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            switch (item.getItemId()) {
                case R.id.category_context_rename:
                    showRenameDialog(c);
                    break;
                case R.id.category_context_delete:
                    showDeleteDialog(c);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    private void showRenameDialog(final Category c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View v = View.inflate(this, R.layout.rename_category, null);

        builder
                .setTitle(getString(R.string.category_context_rename))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText et = v.findViewById(R.id.new_category_name);
                        String categoryName = et.getText().toString().trim();

                        if (categoryName.isEmpty()) {
                            Toast.makeText(CategoryActivity.this, "You must enter a category name", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        c.Name = categoryName;

                        DBHelper.EditCategory(c, c.State.equals(DBHelper.CREATED_STATE_KEY) ? DBHelper.CREATED_STATE_KEY : DBHelper.EDITED_STATE_KEY);

                        loadData();

                        SyncService.startSync(CategoryActivity.this);
                    }
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
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        c.IsDeleted = true;

                        DBHelper.EditCategory(c, c.State.equals(DBHelper.CREATED_STATE_KEY) ? DBHelper.CREATED_STATE_KEY : DBHelper.EDITED_STATE_KEY);

                        ListView lv = findViewById(R.id.category_list);

                        ((CategoryAdapter) lv.getAdapter()).remove(c);

                        SyncService.startSync(CategoryActivity.this);
                    }
                });

        Dialog d = builder.create();

        d.show();
    }

}
