package com.andrewovens.weeklybudget2;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class JoinBudgetActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_budget);
    }

    public void goButtonOnClick(View v) {
        EditText id = findViewById(R.id.text_join_unique_id);

        // Budget IDs are lowercase UUIDs, so the fold is over a fixed machine
        // format and must not follow the device locale (a Turkish locale maps
        // 'I' to a dotless 'ı').
        final String budgetId = id.getText().toString().trim().toLowerCase(Locale.ROOT);

        if (budgetId.isEmpty()) {
            Toast.makeText(this, R.string.error_budget_id_required, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Budget budget = API.GetBudget(budgetId);

                Settings.setBudget(JoinBudgetActivity.this, budget);

                Budget[] budgets = Settings.getBudgets(JoinBudgetActivity.this);
                Budget[] newBudgets;
                if (budgets != null) {
                    newBudgets = new Budget[budgets.length + 1];
                    System.arraycopy(budgets, 0, newBudgets, 0, budgets.length);
                    newBudgets[budgets.length] = budget;
                } else {
                    newBudgets = new Budget[]{budget};
                }

                Settings.setBudgets(JoinBudgetActivity.this, newBudgets);

                runOnUiThread(() -> {
                    JoinBudgetActivity.this.setResult(RESULT_OK);
                    JoinBudgetActivity.this.finish();
                });

            } catch (Exception e) {
                Helpers.showNetworkErrorToastOnUi(JoinBudgetActivity.this, R.string.error_network);
                e.printStackTrace();
            }
        }).start();
    }

}
