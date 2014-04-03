package com.andrewovens.weeklybudget;

import java.io.*;
import java.net.*;

import org.json.*;

public class NetworkOperations {
	public static String HttpGet(URL url) throws IOException
	{
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
			}
			return line;
		}
    	finally {
    		urlConnection.disconnect();
    	}
	}
	
	public static String HttpPost(URL url, String content, String method) throws IOException
	{
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		try {
			urlConnection.setDoOutput(true);
		    urlConnection.setChunkedStreamingMode(0);
		    urlConnection.setRequestMethod(method);
		    urlConnection.setRequestProperty("Content-Type", "application/json");
		    
		    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
		    w.write(content);
		    
			BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
			}
			return line;
		}
    	finally {
    		urlConnection.disconnect();
    	}
	}
}
