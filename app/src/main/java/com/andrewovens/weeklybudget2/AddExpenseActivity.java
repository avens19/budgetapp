package com.andrewovens.weeklybudget2;

import java.util.*;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class AddExpenseActivity extends Activity implements AdapterView.OnItemSelectedListener, DatePicker.OnDateChangedListener {

    private boolean _isEdit = false;
    private Expense _expense;
    private final int NEW_ITEM_INDEX = -2;
    private final int NONE_ITEM_INDEX = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        Intent i = this.getIntent();
        String expenseString = i.getStringExtra("expense");
        DatePicker dp = findViewById(R.id.add_date);
        TextView dateLabel = findViewById(R.id.label_add_date);

        if (expenseString != null) {
            try {
                _isEdit = true;
                _expense = Expense.fromJson(new JSONObject(expenseString));
                Calendar c = Calendar.getInstance();
                c.setTime(_expense.Date);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                String dow = Dates.getLongWeekDay(_expense.Date);
                dp.init(year, month, day, this);
                dateLabel.setText(getString(R.string.add_date, dow));
                this.setTitle(R.string.edit_expense_title);
                EditText description = findViewById(R.id.add_description);
                description.setText(_expense.Description);
                EditText amount = findViewById(R.id.add_amount);
                amount.setText(Helpers.doubleString(_expense.Amount));
                initCategories(_expense.CategoryId);
                Button edit = findViewById(R.id.add_add_button);
                edit.setText(R.string.edit_button);
            } catch (Exception e) {
                this.finish();
            }
        } else {
            initCategories(null);

            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            String dow = Dates.getLongWeekDay(c.getTime());
            dateLabel.setText(getString(R.string.add_date, dow));
            dp.init(year, month, day, this);
        }
    }

    private void initCategories(Long categoryId) {
        Spinner s = findViewById(R.id.category_picker);
        s.setOnItemSelectedListener(this);
        List<Category> categories;

        try {
            Budget budget = Settings.getBudget(this);
            assert budget != null;
            String budgetId = budget.UniqueId;
            categories = DBHelper.GetActiveCategories(budgetId, categoryId);
            Category cat = new Category(getString(R.string.label_no_category), budgetId);
            cat.Id = NONE_ITEM_INDEX;
            categories.add(0, cat);
            cat = new Category(getString(R.string.label_new_category), budgetId);
            cat.Id = NEW_ITEM_INDEX;
            categories.add(cat);

            CategoryAdapter ca = new CategoryAdapter(this, R.layout.category_row, categories);
            ca.setDropDownViewResource(R.layout.category_row);
            s.setAdapter(ca);

            if (categoryId != null) {
                int position = 0;
                for (int index = 0; index < categories.size(); index++) {
                    Category c = categories.get(index);
                    if (c.Id == categoryId) {
                        position = index;
                        break;
                    }
                }
                s.setSelection(position);
            }
        } catch (Exception e) {
            this.finish();
        }
    }

    public void addButtonOnClick(View v) {
        try {
            Budget budget = Settings.getBudget(this);
            assert budget != null;
            String budgetId = budget.UniqueId;
            final Expense e = new Expense();
            DatePicker dp = findViewById(R.id.add_date);
            e.Date = new GregorianCalendar(dp.getYear(), dp.getMonth(), dp.getDayOfMonth()).getTime();
            EditText description = findViewById(R.id.add_description);

            String descriptionString = description.getText().toString();
            descriptionString = descriptionString.trim();

            if (descriptionString.isEmpty()) {
                Toast.makeText(this, "You must enter a description", Toast.LENGTH_SHORT).show();
                return;
            }

            e.Description = descriptionString;
            EditText amount = findViewById(R.id.add_amount);
            String amountString = amount.getText().toString();
            amountString = amountString.trim();

            if (amountString.isEmpty()) {
                Toast.makeText(this, "You must enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                e.Amount = Double.parseDouble(amountString);
            } catch (Exception ex) {
                Toast.makeText(this, "Amount must be a valid decimal number", Toast.LENGTH_SHORT).show();
                return;
            }

            Spinner s = findViewById(R.id.category_picker);
            Category c = (Category) s.getSelectedItem();

            if (c.Id == NEW_ITEM_INDEX) {
                EditText newCategoryTextBox = findViewById(R.id.add_category);
                String categoryName = newCategoryTextBox.getText().toString().trim();

                if (categoryName.isEmpty()) {
                    Toast.makeText(this, "You must enter a category name", Toast.LENGTH_SHORT).show();
                    return;
                }

                c = new Category(categoryName, budgetId);
                c.Id = Settings.getNextCategoryId(this);
                c.IsDeleted = false;

                DBHelper.AddCategory(c, DBHelper.CREATED_STATE_KEY);
            }

            if (c.Id != NONE_ITEM_INDEX) {
                e.CategoryId = c.Id;
            }

            e.BudgetId = budgetId;

            if (_isEdit) {
                e.Id = _expense.Id;

                Expense savedExpense = DBHelper.GetExpense(e.Id);
                if (savedExpense != null) {
                    String state = savedExpense.State;

                    if (state.equals(DBHelper.CREATED_STATE_KEY)) {
                        DBHelper.EditExpense(e, DBHelper.CREATED_STATE_KEY);
                    } else {
                        DBHelper.EditExpense(e, DBHelper.EDITED_STATE_KEY);
                    }
                } else {
                    Helpers.showNetworkErrorToastOnUi(this, R.string.error_cant_edit);
                }

            } else {
                e.Id = Settings.getNextId(this);

                DBHelper.AddExpense(e, DBHelper.CREATED_STATE_KEY);
            }

            SyncService.startSync(this);

        } finally {
            this.finish();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        View addCategory = this.findViewById(R.id.add_category);

        Category c = (Category) parent.getItemAtPosition(position);
        if (c.Id == NEW_ITEM_INDEX) {
            addCategory.setVisibility(View.VISIBLE);
        } else {
            addCategory.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        View addCategory = this.findViewById(R.id.add_category);

        addCategory.setVisibility(View.GONE);
    }

    @Override
    public void onDateChanged(DatePicker datePicker, int year, int month, int day) {
        Date d = new GregorianCalendar(year, month, day).getTime();
        TextView dateLabel = findViewById(R.id.label_add_date);
        String dow = Dates.getLongWeekDay(d);
        dateLabel.setText(getString(R.string.add_date, dow));
    }
}
