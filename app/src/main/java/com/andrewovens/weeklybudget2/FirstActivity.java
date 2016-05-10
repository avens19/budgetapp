package com.andrewovens.weeklybudget2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class FirstActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_first);
	}
	
	public void newBudgetOnClick(View view)
	{
		Intent i = new Intent(this, NewBudgetActivity.class);
		startActivityForResult(i,1);
	}
	
	public void joinBudgetOnClick(View view)
	{
		Intent i = new Intent(this, JoinBudgetActivity.class);
		startActivityForResult(i,1);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if(resultCode == Activity.RESULT_OK)
			this.finish();
	}
}
