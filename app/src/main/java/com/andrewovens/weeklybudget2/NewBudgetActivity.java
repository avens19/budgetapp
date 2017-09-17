package com.andrewovens.weeklybudget2;

import org.json.JSONObject;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class NewBudgetActivity extends Activity {
	
	private Budget _budget;
	private boolean _isEdit = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_budget);
		
		Intent i = getIntent();
		String budget = i.getStringExtra("budget");
		
		TextView uniqueId = (TextView)findViewById(R.id.text_new_unique);
		
		if(budget == null)
		{		
			_budget = new Budget(true);
			
			uniqueId.setText(_budget.UniqueId);
		}
		else
		{
			try
			{
				_isEdit = true;
				this.setTitle(R.string.edit_budget_title);
				
				_budget = Budget.fromJson(new JSONObject(budget));

				EditText name = (EditText)findViewById(R.id.text_budget_name);
				Spinner weekday = (Spinner)findViewById(R.id.weekday_spinner);
				EditText amount = (EditText)findViewById(R.id.text_new_amount);
				Button edit = (Button)findViewById(R.id.button_create_budget);

				name.setText(_budget.Name);
				weekday.setSelection(_budget.StartDay);
				amount.setText(Helpers.doubleString(_budget.Amount));
				uniqueId.setText(_budget.UniqueId);
				edit.setText(R.string.button_edit_budget);
			}
			catch(Exception e)
			{
				this.finish();
			}
		}
	}
	
	public void uniqueIdOnClick(View v)
	{
		ClipboardManager clipboard = (ClipboardManager)
		        getSystemService(Context.CLIPBOARD_SERVICE);
		
		clipboard.setPrimaryClip(ClipData.newPlainText("uniqueId", _budget.UniqueId));
		
		Toast.makeText(this, R.string.copied_unique_id, Toast.LENGTH_SHORT).show();
	}
	
	public void goButtonOnClick(View v)
	{
		EditText name = (EditText)findViewById(R.id.text_budget_name);
		Spinner weekday = (Spinner)findViewById(R.id.weekday_spinner);
		EditText amount = (EditText)findViewById(R.id.text_new_amount);
		
		_budget.StartDay = weekday.getSelectedItemPosition();

		String nameString = name.getText().toString();
		String amountString = amount.getText().toString();
		amountString = amountString.trim();

		if(nameString.isEmpty()) {
            Toast.makeText(this, "You must enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        _budget.Name = nameString;

		if(amountString.isEmpty()) {
            Toast.makeText(this, "You must enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }
		
		try
		{
			_budget.Amount = Double.parseDouble(amount.getText().toString());
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Amount must be a valid decimal number", Toast.LENGTH_SHORT).show();
			return;
		}
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Looper.prepare();
					
					if(_isEdit)
					{
						API.EditBudget(_budget);
						
						Settings.setBudget(NewBudgetActivity.this, _budget);

                        Budget[] budgets = Settings.getBudgets(NewBudgetActivity.this);
                        Budget[] newBudgets;
                        if(budgets != null){
                            newBudgets = new Budget[budgets.length];
                            for (int i = 0;i<budgets.length;i++){
                                if(budgets[i].UniqueId.equals(_budget.UniqueId)) {
                                    newBudgets[i] = _budget;
                                }else{
                                    newBudgets[i] = budgets[i];
                                }
                            }
                        }else{
                            newBudgets = new Budget[1];
                            newBudgets[0] = _budget;
                        }

                        Settings.setBudgets(NewBudgetActivity.this, newBudgets);
						
						NewBudgetActivity.this.finish();
					}
					else
					{
						_budget = API.CreateBudget(_budget);
						
						Settings.setBudget(NewBudgetActivity.this, _budget);

                        Budget[] budgets = Settings.getBudgets(NewBudgetActivity.this);
                        Budget[] newBudgets;
                        if(budgets != null){
                            newBudgets = new Budget[budgets.length + 1];
                            for (int i = 0;i<budgets.length;i++){
                                newBudgets[i] = budgets[i];
                            }
                            newBudgets[budgets.length] = _budget;
                        }else{
                            newBudgets = new Budget[1];
                            newBudgets[0] = _budget;
                        }

                        Settings.setBudgets(NewBudgetActivity.this, newBudgets);

						NewBudgetActivity.this.setResult(RESULT_OK);
						NewBudgetActivity.this.finish();
					}
					
				} catch (Exception e) {
					Helpers.showToastOnUi(NewBudgetActivity.this, R.string.error_network, Toast.LENGTH_SHORT);
					e.printStackTrace();
				}
			}
			
		}).start();
	}
}
