package com.andrewovens.weeklybudget2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Add or edit one expense.
 *
 * <p>The form leads with the amount, which is the only field that always has
 * to be typed. The date was an inline {@code DatePicker} that took up most of
 * the screen and pushed the description and amount below the fold; it is now a
 * field that opens {@link MaterialDatePicker}.
 */
public class AddExpenseActivity extends BaseActivity {

    private boolean _isEdit = false;
    private Expense _expense;
    private Calendar _date;
    private List<Category> _categories = new ArrayList<>();
    private Category _selectedCategory;

    private static final int NEW_ITEM_INDEX = -2;
    private static final int NONE_ITEM_INDEX = -1;

    private static final String DATE_PICKER_TAG = "expense_date_picker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        MaterialButton save = findViewById(R.id.add_add_button);
        save.setOnClickListener(this::addButtonOnClick);

        // The date field opens a picker instead of taking text, but it stays
        // focusable so it is still reachable by keyboard and D-pad; the soft
        // keyboard is suppressed here because there is no XML attribute for it.
        TextInputEditText dateField = findViewById(R.id.add_date);
        dateField.setShowSoftInputOnFocus(false);
        dateField.setOnClickListener(v -> showDatePicker());
        TextInputLayout dateLayout = findViewById(R.id.add_date_layout);
        dateLayout.setEndIconOnClickListener(v -> showDatePicker());

        String expenseString = getIntent().getStringExtra("expense");

        if (expenseString != null) {
            try {
                _isEdit = true;
                _expense = Expense.fromJson(new JSONObject(expenseString));

                _date = Calendar.getInstance();
                _date.setTime(_expense.Date);

                BudgetTitle.asSubtitle(this, R.string.edit_expense_title, budgetOrNull());
                save.setText(R.string.edit_button);

                ((EditText) findViewById(R.id.add_description)).setText(_expense.Description);
                ((EditText) findViewById(R.id.add_amount)).setText(Helpers.doubleString(_expense.Amount));

                initCategories(_expense.CategoryId);
            } catch (Exception e) {
                this.finish();
                return;
            }
        } else {
            BudgetTitle.asSubtitle(this, R.string.title_activity_add_expense, budgetOrNull());
            _date = Calendar.getInstance();
            initCategories(null);
        }

        bindDate();
        focusAmount();
    }

    /**
     * The amount is the one field every expense needs, so the form opens on it
     * with the keyboard showing. On an edit the existing value is selected, so
     * typing replaces it instead of appending to it.
     */
    private void focusAmount() {
        final EditText amount = findViewById(R.id.add_amount);
        amount.requestFocus();
        if (_isEdit) {
            amount.selectAll();
        }
        // Posted, because the window has no insets controller until it is
        // attached, and asking for the IME before that is a no-op.
        amount.post(() -> {
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(getWindow(), amount);
            controller.show(WindowInsetsCompat.Type.ime());
        });
    }

    private Budget budgetOrNull() {
        return Settings.getBudget(this);
    }

    private void bindDate() {
        TextInputEditText field = findViewById(R.id.add_date);
        field.setText(Dates.getFullDateString(this, _date.getTime()));
    }

    private void showDatePicker() {
        // The picker works in UTC and hands back UTC midnight, so the local
        // calendar date has to be converted in both directions or an expense
        // entered west of Greenwich lands on the previous day.
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_date)
                .setSelection(toUtcMillis(_date))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            _date = fromUtcMillis(selection);
            bindDate();
        });

        picker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
    }

    private static long toUtcMillis(Calendar local) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    private static Calendar fromUtcMillis(long millis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(millis);

        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local;
    }

    private void initCategories(Long categoryId) {
        MaterialAutoCompleteTextView picker = findViewById(R.id.category_picker);

        try {
            Budget budget = Settings.getBudget(this);
            if (budget == null) {
                this.finish();
                return;
            }
            String budgetId = budget.UniqueId;

            _categories = DBHelper.GetActiveCategories(budgetId, categoryId);

            Category none = new Category(getString(R.string.label_no_category), budgetId);
            none.Id = NONE_ITEM_INDEX;
            _categories.add(0, none);

            Category create = new Category(getString(R.string.label_new_category), budgetId);
            create.Id = NEW_ITEM_INDEX;
            _categories.add(create);

            String[] names = new String[_categories.size()];
            for (int i = 0; i < _categories.size(); i++) {
                names[i] = _categories.get(i).Name;
            }
            picker.setSimpleItems(names);

            int selected = 0;
            if (categoryId != null) {
                for (int i = 0; i < _categories.size(); i++) {
                    if (_categories.get(i).Id == categoryId) {
                        selected = i;
                        break;
                    }
                }
            }

            picker.setOnItemClickListener((parent, view, position, id) -> selectCategory(position));
            selectCategory(selected);
        } catch (Exception e) {
            this.finish();
        }
    }

    private void selectCategory(int position) {
        _selectedCategory = _categories.get(position);

        MaterialAutoCompleteTextView picker = findViewById(R.id.category_picker);
        picker.setText(_selectedCategory.Name, false);

        findViewById(R.id.add_category_layout).setVisibility(
                _selectedCategory.Id == NEW_ITEM_INDEX ? View.VISIBLE : View.GONE);
    }

    /**
     * Saves the expense and closes the screen.
     *
     * <p>The save used to sit in a {@code try/finally} that called
     * {@code finish()} unconditionally, so failing validation showed a toast
     * and then closed the form anyway, throwing away everything the user had
     * typed. Each validation failure now returns and leaves the form open.
     */
    public void addButtonOnClick(View v) {
        Budget budget = Settings.getBudget(this);
        if (budget == null) {
            this.finish();
            return;
        }
        String budgetId = budget.UniqueId;

        final Expense e = new Expense();
        e.Date = new GregorianCalendar(_date.get(Calendar.YEAR), _date.get(Calendar.MONTH),
                _date.get(Calendar.DAY_OF_MONTH)).getTime();

        TextInputLayout descriptionLayout = findViewById(R.id.add_description_layout);
        EditText description = findViewById(R.id.add_description);
        String descriptionString = description.getText().toString().trim();

        descriptionLayout.setError(null);
        if (descriptionString.isEmpty()) {
            descriptionLayout.setError(getString(R.string.error_description_required));
            description.requestFocus();
            return;
        }

        e.Description = descriptionString;

        EditText amount = findViewById(R.id.add_amount);
        String amountString = amount.getText().toString().trim();

        if (amountString.isEmpty()) {
            Toast.makeText(this, R.string.error_amount_required, Toast.LENGTH_SHORT).show();
            amount.requestFocus();
            return;
        }
        try {
            e.Amount = Double.parseDouble(amountString.replace(',', '.'));
        } catch (NumberFormatException ex) {
            Toast.makeText(this, R.string.error_amount_invalid, Toast.LENGTH_SHORT).show();
            amount.requestFocus();
            return;
        }

        Category c = _selectedCategory;

        if (c.Id == NEW_ITEM_INDEX) {
            TextInputLayout newCategoryLayout = findViewById(R.id.add_category_layout);
            EditText newCategoryTextBox = findViewById(R.id.add_category);
            String categoryName = newCategoryTextBox.getText().toString().trim();

            newCategoryLayout.setError(null);
            if (categoryName.isEmpty()) {
                newCategoryLayout.setError(getString(R.string.error_category_name_required));
                newCategoryTextBox.requestFocus();
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

                if (DBHelper.CREATED_STATE_KEY.equals(state)) {
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

        Sync.start(this);

        this.finish();
    }
}
