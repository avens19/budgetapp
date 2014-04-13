package com.andrewovens.weeklybudget;

import java.util.Calendar;
import java.util.Date;

public class DateTotal {
	public Calendar Date;
	public double Total;
	
	public DateTotal(Calendar date, double total)
	{
		Date = (Calendar)date.clone();
		Total = total;
	}
}
