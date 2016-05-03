package com.andrewovens.weeklybudget2;

import java.util.*;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class AddExpenseActivity extends Activity implements AdapterView.OnItemSelectedListener {
	
	private boolean _isEdit = false;
	private Expense _expense;
    private final int NEWITEMINDEX = -2;
    private final int NONEITEMINDEX = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_expense);
		
		Intent i = this.getIntent();
		String expenseString = i.getStringExtra("expense");
		DatePicker dp = (DatePicker)findViewById(R.id.add_date);
		
		if(expenseString != null)
		{
			try
			{
				_isEdit = true;
				_expense = Expense.fromJson(new JSONObject(expenseString));
				Calendar c = Calendar.getInstance();
				c.setTime(_expense.Date);
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH);
				int day = c.get(Calendar.DAY_OF_MONTH);
				dp.init(year, month, day, null);
				this.setTitle(R.string.edit_expense_title);
				EditText description = (EditText)findViewById(R.id.add_description);
				description.setText(_expense.Description);
				EditText amount = (EditText)findViewById(R.id.add_amount);
				amount.setText(Helpers.doubleString(_expense.Amount));
				initCategories(_expense.CategoryId);
				Button edit = (Button)findViewById(R.id.add_add_button);
				edit.setText(R.string.edit_button);
			}
			catch(Exception e)
			{
				this.finish();
			}
		}
		else
		{
			initCategories(null);

			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			
			dp.init(year, month, day, null);
		}
	}

	private void initCategories(Long categoryId)
	{
		Spinner s = (Spinner)findViewById(R.id.category_picker);
		s.setOnItemSelectedListener(this);
		List<Category> categories = new ArrayList<Category>();

		try
		{
			String budgetId = Settings.getBudget(this).UniqueId;
			categories = DBHelper.GetActiveCategories(budgetId, categoryId);
			Category cat = new Category(getString(R.string.label_no_category), budgetId);
			cat.Id = NONEITEMINDEX;
			categories.add(0, cat);
			cat = new Category(getString(R.string.label_new_category), budgetId);
			cat.Id = NEWITEMINDEX;
			categories.add(cat);

			CategoryAdapter ca = new CategoryAdapter(this, R.layout.category_row, categories);
			ca.setDropDownViewResource(R.layout.category_row);
			s.setAdapter(ca);

			if(categoryId != null)
			{
				int position = 0;
				for(int index = 0; index < categories.size(); index++)
				{
					Category c = categories.get(index);
					if(c.Id == categoryId)
					{
						position = index;
						break;
					}
				}
				s.setSelection(position);
			}
		}
		catch (Exception e)
		{
			this.finish();
		}
	}
	
	public void addButtonOnClick(View v)
	{
        String budgetId = Settings.getBudget(this).UniqueId;
		final Expense e = new Expense();
		DatePicker dp = (DatePicker)findViewById(R.id.add_date);
		e.Date = new GregorianCalendar(dp.getYear(), dp.getMonth(), dp.getDayOfMonth()).getTime();
		EditText description = (EditText)findViewById(R.id.add_description);
		
		String descriptionString = description.getText().toString();
		descriptionString = descriptionString.trim();
		
		if(descriptionString.isEmpty())
		{
			Toast.makeText(this, "You must enter a description", Toast.LENGTH_SHORT).show();
			return;
		}
		
		e.Description = descriptionString;
		EditText amount = (EditText)findViewById(R.id.add_amount);
		String amountString = amount.getText().toString();
		amountString = amountString.trim();
		
		if(amountString.isEmpty())
		{
			Toast.makeText(this, "You must enter an amount", Toast.LENGTH_SHORT).show();
			return;
		}
		try
		{
			e.Amount = Double.parseDouble(amountString);
		}
		catch(Exception ex)
		{
			Toast.makeText(this, "Amount must be a valid decimal number", Toast.LENGTH_SHORT).show();
			return;
		}

        Spinner s = (Spinner)findViewById(R.id.category_picker);
        Category c = (Category)s.getSelectedItem();

        if(c.Id == NEWITEMINDEX){
            EditText newCategoryTextBox = (EditText)findViewById(R.id.add_category);
            String categoryName = newCategoryTextBox.getText().toString().trim();

            if(categoryName.isEmpty())
            {
                Toast.makeText(this, "You must enter a category name", Toast.LENGTH_SHORT).show();
                return;
            }

            c = new Category(categoryName, budgetId);
            c.Id = Settings.getNextCategoryId(this);
            c.IsDeleted = false;

            DBHelper.AddCategory(c, "created");
        }

        if(c.Id != NONEITEMINDEX){
            e.CategoryId = c.Id;
        }
		
		e.BudgetId = budgetId;
		
		if(_isEdit)
		{
			e.Id = _expense.Id;
			String state = DBHelper.GetExpense(e.Id).State;
			
			if(state.equals("created"))
			{
				DBHelper.EditExpense(e, "created");
			}
			else
			{
				DBHelper.EditExpense(e, "edited");
			}
			
		}
		else
		{
			e.Id = Settings.getNextId(this);
			
			DBHelper.AddExpense(e, "created");
		}
		
		this.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add_expense, menu);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        View addCategory = this.findViewById(R.id.add_category);

        Category c = (Category)parent.getItemAtPosition(position);
        if(c.Id == NEWITEMINDEX)
        {
            addCategory.setVisibility(View.VISIBLE);
        }
        else
        {
            addCategory.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        View addCategory = this.findViewById(R.id.add_category);

        addCategory.setVisibility(View.GONE);
    }
}
