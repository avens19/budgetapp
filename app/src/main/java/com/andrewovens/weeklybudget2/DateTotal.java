package com.andrewovens.weeklybudget2;

import java.util.Calendar;

public class DateTotal {
	public Calendar Date;
	public double Total;
	
	public DateTotal(Calendar date, double total)
	{
		Date = (Calendar)date.clone();
		Total = total;
	}
}
