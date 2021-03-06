package com.andrewovens.weeklybudget2;

import android.support.annotation.NonNull;

import java.io.*;
import java.net.*;

class NetworkOperations {
    @NonNull
    static String HttpGet(URL url) throws IOException {
        return HttpGet(url, "GET");
    }

    @NonNull
    static String HttpGet(@NonNull URL url, String method) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setRequestMethod(method);

            BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        } finally {
            urlConnection.disconnect();
        }
    }

    @NonNull
    static String HttpPost(URL url, String content) throws IOException {
        return HttpPost(url, content, "POST");
    }

    @NonNull
    static String HttpPost(@NonNull URL url, String content, String method) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod(method);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
            w.write(content);
            w.flush();
            w.close();

            BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        } finally {
            urlConnection.disconnect();
        }
    }
}
