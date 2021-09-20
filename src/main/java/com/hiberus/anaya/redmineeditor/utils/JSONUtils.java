package com.hiberus.anaya.redmineeditor.utils;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @see JSONUtils#getFromUrl(String)
 * From https://stackoverflow.com/a/4308662
 */
public class JSONUtils {

    /**
     * Reads JSON data from an url
     *
     * @param url url to load
     * @return the json data returned
     * @throws IOException on network errors
     */
    public static JSONObject getFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return new JSONObject(readAll(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))));
        }
    }

    public static int putToUrl(String url, JSONObject object) {
        try {
            URL _url = new URL(url);
            HttpURLConnection http = (HttpURLConnection) _url.openConnection();
            http.setRequestMethod("PUT");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");

            http.getOutputStream().write(object.toString().getBytes(StandardCharsets.UTF_8));

            int responseCode = http.getResponseCode();
            http.disconnect();
            return responseCode;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int postToUrl(String url, JSONObject object) {
        try {
            URL _url = new URL(url);
            HttpURLConnection http = (HttpURLConnection) _url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");

            http.getOutputStream().write(object.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println(http.getResponseMessage());
            int responseCode = http.getResponseCode();
            http.disconnect();
            return responseCode;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int delete(String url) {
        try {
            URL _url = new URL(url);
            HttpURLConnection http = (HttpURLConnection) _url.openConnection();
            http.setRequestMethod("DELETE");
            int responseCode = http.getResponseCode();
            http.disconnect();
            return responseCode;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ------------------------- utils -------------------------

    private static String readAll(Reader rd) throws IOException {
        // from https://www.baeldung.com/java-convert-reader-to-string
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = rd.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        return buffer.toString();
    }
}
