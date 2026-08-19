package com.andrewovens.weeklybudget2;

import android.os.Bundle;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class JoinBudgetActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_budget);

        MaterialButton go = findViewById(R.id.button_go);
        go.setOnClickListener(v -> join(go));
    }

    private void join(MaterialButton go) {
        EditText id = findViewById(R.id.text_join_unique_id);
        TextInputLayout layout = findViewById(R.id.join_id_layout);

        // Budget IDs are lowercase UUIDs, so the fold is over a fixed machine
        // format and must not follow the device locale (a Turkish locale maps
        // 'I' to a dotless 'ı').
        final String budgetId = id.getText().toString().trim().toLowerCase(Locale.ROOT);

        layout.setError(null);
        if (budgetId.isEmpty()) {
            layout.setError(getString(R.string.error_budget_id_required));
            return;
        }

        go.setEnabled(false);

        new Thread(() -> {
            try {
                Budget budget = API.GetBudget(budgetId);

                // Shared with the invite-link path, and it replaces rather than
                // appends: joining a budget this device already had used to put
                // it in the switcher twice.
                Settings.rememberBudget(JoinBudgetActivity.this, budget);

                runOnUiThread(() -> {
                    JoinBudgetActivity.this.setResult(RESULT_OK);
                    JoinBudgetActivity.this.finish();
                });

            } catch (Exception e) {
                Helpers.showNetworkErrorToastOnUi(JoinBudgetActivity.this, R.string.error_network);
                runOnUiThread(() -> go.setEnabled(true));
                e.printStackTrace();
            }
        }).start();
    }
}
