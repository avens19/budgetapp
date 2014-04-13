package com.andrewovens.weeklybudget;

import android.app.Activity;
import android.widget.Toast;

public class Helpers {
	public static void showToastOnUi(final Activity a, final int resId, final int length)
	{
		a.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				Toast.makeText(a, resId, length).show();
			}
			
		});
	}
}
