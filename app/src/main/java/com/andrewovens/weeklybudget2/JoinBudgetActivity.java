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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.join_budget, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
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
					
					Intent i = new Intent(JoinBudgetActivity.this, WeekActivity.class);
					startActivity(i);
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
