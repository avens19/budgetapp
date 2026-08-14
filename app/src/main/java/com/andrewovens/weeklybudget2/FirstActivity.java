package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class FirstActivity extends BaseActivity {

    private static final int CREATE_OR_JOIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
    }

    public void newBudgetOnClick(View view) {
        Intent i = new Intent(this, NewBudgetActivity.class);
        startActivityForResult(i, CREATE_OR_JOIN);
    }

    public void joinBudgetOnClick(View view) {
        Intent i = new Intent(this, JoinBudgetActivity.class);
        startActivityForResult(i, CREATE_OR_JOIN);
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
