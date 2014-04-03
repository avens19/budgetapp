package com.andrewovens.weeklybudget;

import java.util.*;

public class Expense {
	public long id;
	public Date Date;
	public String Description;
	public double Amount;
	public String Budget;
	
	public Expense(Date date, String description, double amount, String budget)
	{
		Date = date;
		Description = description;
		Amount = amount;
		Budget = budget;
	}
}
