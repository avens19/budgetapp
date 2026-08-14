package com.andrewovens.weeklybudget2;

import org.json.JSONObject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class NewBudgetActivity extends BaseActivity {

    private Budget _budget;
    private boolean _isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_budget);

        Intent i = getIntent();
        String budget = i.getStringExtra("budget");

        TextView uniqueId = findViewById(R.id.text_new_unique);

        if (budget == null) {
            _budget = new Budget(true);

            uniqueId.setText(_budget.UniqueId);
        } else {
            try {
                _isEdit = true;
                this.setTitle(R.string.edit_budget_title);

                _budget = Budget.fromJson(new JSONObject(budget));

                EditText name = findViewById(R.id.text_budget_name);
                Spinner weekday = findViewById(R.id.weekday_spinner);
                EditText amount = findViewById(R.id.text_new_amount);
                Button edit = findViewById(R.id.button_create_budget);

                name.setText(_budget.Name);
                weekday.setSelection(_budget.StartDay);
                amount.setText(Helpers.doubleString(_budget.Amount));
                uniqueId.setText(_budget.UniqueId);
                edit.setText(R.string.button_edit_budget);
            } catch (Exception e) {
                this.finish();
            }
        }
    }

    public void uniqueIdOnClick(View v) {
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        clipboard.setPrimaryClip(ClipData.newPlainText("uniqueId", _budget.UniqueId));

        Toast.makeText(this, R.string.copied_unique_id, Toast.LENGTH_SHORT).show();
    }

    public void goButtonOnClick(View v) {
        EditText name = findViewById(R.id.text_budget_name);
        Spinner weekday = findViewById(R.id.weekday_spinner);
        EditText amount = findViewById(R.id.text_new_amount);

        String nameString = name.getText().toString().trim();
        String amountString = amount.getText().toString().trim();

        if (nameString.isEmpty()) {
            Toast.makeText(this, R.string.error_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountString.isEmpty()) {
            Toast.makeText(this, R.string.error_amount_required, Toast.LENGTH_SHORT).show();
            return;
        }

        double amountValue;
        try {
            amountValue = Double.parseDouble(amountString.replace(',', '.'));
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_amount_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        _budget.StartDay = weekday.getSelectedItemPosition();
        _budget.Name = nameString;
        _budget.Amount = amountValue;

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
