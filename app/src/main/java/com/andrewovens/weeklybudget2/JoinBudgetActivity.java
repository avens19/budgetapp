package com.andrewovens.weeklybudget2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint("DefaultLocale")
public class JoinBudgetActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join_budget);
	}
	
	public void goButtonOnClick(View v)
	{
		EditText id = (EditText)findViewById(R.id.text_join_unique_id);
		
		final String budgetId = id.getText().toString().toLowerCase();
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Looper.prepare();
					
					Budget budget = API.GetBudget(budgetId);
					
					Settings.setBudget(JoinBudgetActivity.this, budget);

                    Budget[] budgets = Settings.getBudgets(JoinBudgetActivity.this);
                    Budget[] newBudgets;
                    if(budgets != null){
                        newBudgets = new Budget[budgets.length + 1];
                        for (int i = 0;i<budgets.length;i++){
                            newBudgets[i] = budgets[i];
                        }
                        newBudgets[budgets.length] = budget;
                    }else{
                        newBudgets = new Budget[1];
                        newBudgets[0] = budget;
                    }

                    Settings.setBudgets(JoinBudgetActivity.this, newBudgets);

					JoinBudgetActivity.this.setResult(RESULT_OK);
					JoinBudgetActivity.this.finish();
					
				} catch (Exception e) {
					Helpers.showToastOnUi(JoinBudgetActivity.this, R.string.error_network, Toast.LENGTH_SHORT);
					e.printStackTrace();
				}
			}
			
		}).start();
	}

}
