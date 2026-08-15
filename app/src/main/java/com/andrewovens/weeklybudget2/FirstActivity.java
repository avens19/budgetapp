package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FirstActivity extends BaseActivity {

    private static final int CREATE_OR_JOIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        // First run only. Launched from here rather than from WeekActivity so
        // that it appears before the create-or-join choice, which is the point
        // at which "what is this app for?" needs answering.
        if (!Settings.hasSeenTutorial(this)) {
            Settings.setSeenTutorial(this);
            startActivity(new Intent(this, TutorialActivity.class));
        }

        findViewById(R.id.new_budget).setOnClickListener(v ->
                startActivityForResult(new Intent(this, NewBudgetActivity.class), CREATE_OR_JOIN));

        findViewById(R.id.join_budget).setOnClickListener(v ->
                startActivityForResult(new Intent(this, JoinBudgetActivity.class), CREATE_OR_JOIN));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            this.setResult(RESULT_OK);
            this.finish();
        }
    }
}
