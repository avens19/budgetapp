package com.andrewovens.weeklybudget2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class NetworkOperations {

    /**
     * Without these an unreachable or hanging server blocks the sync thread
     * forever, and the spinner on the week screen never clears.
     */
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    /**
     * A response body plus the headers that came with it.
     *
     * <p>The sync needs the server's clock, which it reads out of a response
     * header, so the plain body-only calls are not enough for those requests.
     */
    static final class Response {
        final String body;
        private final Map<String, List<String>> headers;

        Response(String body, Map<String, List<String>> headers) {
            this.body = body;
            this.headers = headers;
        }

        /** Header field names are case-insensitive, and proxies do re-case them. */
        @Nullable
        String header(String name) {
            if (headers == null) {
                return null;
            }
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.equalsIgnoreCase(name)) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        return values.get(0);
                    }
                }
            }
            return null;
        }
    }

    @NonNull
    static String HttpGet(URL url) throws IOException {
        return HttpGet(url, "GET");
    }

    @NonNull
    static String HttpGet(@NonNull URL url, String method) throws IOException {
        return HttpGetWithHeaders(url, method).body;
    }

    @NonNull
    static Response HttpGetWithHeaders(@NonNull URL url) throws IOException {
        return HttpGetWithHeaders(url, "GET");
    }

    @NonNull
    private static Response HttpGetWithHeaders(@NonNull URL url, String method) throws IOException {
        HttpURLConnection urlConnection = open(url, method);
        try {
            String body = readResponse(urlConnection);
            return new Response(body, urlConnection.getHeaderFields());
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
        HttpURLConnection urlConnection = open(url, method);
        try {
            byte[] body = content.getBytes(StandardCharsets.UTF_8);
            urlConnection.setDoOutput(true);
            urlConnection.setFixedLengthStreamingMode(body.length);
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            try (OutputStream out = urlConnection.getOutputStream()) {
                out.write(body);
            }

            return readResponse(urlConnection);
        } finally {
            urlConnection.disconnect();
        }
    }

    @NonNull
    private static HttpURLConnection open(@NonNull URL url, String method) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(method);
        urlConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        urlConnection.setReadTimeout(READ_TIMEOUT_MS);
        urlConnection.setRequestProperty("Accept", "application/json");
        return urlConnection;
    }

    /**
     * Reads the body as UTF-8. On an error status the body comes from
     * {@code getErrorStream}, so the caller sees the server's message rather
     * than a bare {@code IOException} from {@code getInputStream}.
     */
    @NonNull
    private static String readResponse(@NonNull HttpURLConnection urlConnection) throws IOException {
        int status = urlConnection.getResponseCode();
        InputStream stream = status >= HttpURLConnection.HTTP_BAD_REQUEST
                ? urlConnection.getErrorStream()
                : urlConnection.getInputStream();

        StringBuilder total = new StringBuilder();
        if (stream != null) {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
            }
        }

        if (status >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new IOException("HTTP " + status + " from " + urlConnection.getURL() + ": " + total);
        }

        return total.toString();
    }
}
