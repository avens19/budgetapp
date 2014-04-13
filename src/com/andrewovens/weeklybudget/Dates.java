package com.andrewovens.weeklybudget;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Dates {
	public static String getDateString(Date date)
	{
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		return formatter.format(date);
	}
	public static Date getDate(String date)
	{
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		try {
			return formatter.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new Date();
	}
	public static String getDateTimeString(Date date)
	{
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
		return formatter.format(date);
	}
	public static Date getDateTime(String date)
	{
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
		try {
			return formatter.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new Date();
	}

	public static String UTCTimeString()
	{
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UTC"));

		return dateFormatGmt.format(new Date());
	}

	public static String getWeekDay(Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("EEE");
		return sdf.format(date);
	}

	//Sunday - 0
	public static String getWeekDay(int day)
	{
		switch(day)
		{
		case 0:
			return "Sun";
		case 1:
			return "Mon";
		case 2:
			return "Tue";
		case 3:
			return "Wed";
		case 4:
			return "Thu";
		case 5:
			return "Fri";
		default:
			return "Sat";
		}
	}
}
