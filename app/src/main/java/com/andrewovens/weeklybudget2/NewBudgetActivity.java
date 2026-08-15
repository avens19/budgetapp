package com.andrewovens.weeklybudget2;

import org.json.JSONObject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

public class NewBudgetActivity extends BaseActivity {

    private Budget _budget;
    private boolean _isEdit = false;
    private int _startDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_budget);

        MaterialButton save = findViewById(R.id.button_create_budget);
        save.setOnClickListener(v -> goButtonOnClick(save));

        findViewById(R.id.budget_id_card).setOnClickListener(v -> copyUniqueId());

        String budget = getIntent().getStringExtra("budget");
        TextView uniqueId = findViewById(R.id.text_new_unique);

        if (budget == null) {
            _budget = new Budget(true);
            setTitle(R.string.title_activity_new_budget);
            uniqueId.setText(_budget.UniqueId);
            setStartDay(0);
        } else {
            try {
                _isEdit = true;
                _budget = Budget.fromJson(new JSONObject(budget));

                setTitle(R.string.edit_budget_title);
                save.setText(R.string.button_edit_budget);

                ((EditText) findViewById(R.id.text_budget_name)).setText(_budget.Name);
                ((EditText) findViewById(R.id.text_new_amount)).setText(Helpers.doubleString(_budget.Amount));
                uniqueId.setText(_budget.UniqueId);
                setStartDay(_budget.StartDay);
            } catch (Exception e) {
                this.finish();
                return;
            }
        }

        MaterialAutoCompleteTextView weekday = findViewById(R.id.weekday_spinner);
        weekday.setOnItemClickListener((parent, view, position, id) -> _startDay = position);
    }

    /** Keeps the field text and {@link #_startDay} in step. */
    private void setStartDay(int day) {
        _startDay = day;
        MaterialAutoCompleteTextView weekday = findViewById(R.id.weekday_spinner);
        weekday.setText(getResources().getStringArray(R.array.array_weekdays)[day], false);
    }

    private void copyUniqueId() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("uniqueId", _budget.UniqueId));

        Toast.makeText(this, R.string.copied_unique_id, Toast.LENGTH_SHORT).show();
    }

    private void goButtonOnClick(MaterialButton save) {
        EditText name = findViewById(R.id.text_budget_name);
        EditText amount = findViewById(R.id.text_new_amount);

        String nameString = name.getText().toString().trim();
        String amountString = amount.getText().toString().trim();

        TextInputLayout nameLayout = findViewById(R.id.budget_name_layout);
        nameLayout.setError(null);
        if (nameString.isEmpty()) {
            nameLayout.setError(getString(R.string.error_name_required));
            name.requestFocus();
            return;
        }

        TextInputLayout amountLayout = findViewById(R.id.budget_amount_layout);
        amountLayout.setError(null);
        if (amountString.isEmpty()) {
            amountLayout.setError(getString(R.string.error_amount_required));
            amount.requestFocus();
            return;
        }

        double amountValue;
        try {
            amountValue = Double.parseDouble(amountString.replace(',', '.'));
        } catch (NumberFormatException e) {
            amountLayout.setError(getString(R.string.error_amount_invalid));
            amount.requestFocus();
            return;
        }

        _budget.StartDay = _startDay;
        _budget.Name = nameString;
        _budget.Amount = amountValue;

        save.setEnabled(false);

        // The API call has to leave the main thread; everything that touches
        // the activity (finish, setResult) is posted back to it.
        new Thread(() -> {
            try {
                if (_isEdit) {
                    API.EditBudget(_budget);
                    saveBudget(_budget, false);
                    runOnUiThread(NewBudgetActivity.this::finish);
                } else {
                    final Budget created = API.CreateBudget(_budget);
                    _budget = created;
                    saveBudget(created, true);
                    runOnUiThread(() -> {
                        NewBudgetActivity.this.setResult(RESULT_OK);
                        NewBudgetActivity.this.finish();
                    });
                }
            } catch (Exception e) {
                Helpers.showNetworkErrorToastOnUi(NewBudgetActivity.this, R.string.error_network);
                runOnUiThread(() -> save.setEnabled(true));
                e.printStackTrace();
            }
        }).start();
    }

    private void saveBudget(Budget budget, boolean isNew) throws org.json.JSONException {
        Settings.setBudget(this, budget);

        Budget[] budgets = Settings.getBudgets(this);
        Budget[] newBudgets;

        if (isNew) {
            if (budgets != null) {
                newBudgets = new Budget[budgets.length + 1];
                System.arraycopy(budgets, 0, newBudgets, 0, budgets.length);
                newBudgets[budgets.length] = budget;
            } else {
                newBudgets = new Budget[]{budget};
            }
        } else if (budgets != null) {
            newBudgets = new Budget[budgets.length];
            for (int i = 0; i < budgets.length; i++) {
                newBudgets[i] = budgets[i].UniqueId.equals(budget.UniqueId) ? budget : budgets[i];
            }
        } else {
            newBudgets = new Budget[]{budget};
        }

        Settings.setBudgets(this, newBudgets);
    }
}
