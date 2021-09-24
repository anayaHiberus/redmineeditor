package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for urls with JSON
 */
public class UrlJSON {

    /**
     * Reads JSON data from an url
     * From https://stackoverflow.com/a/4308662
     *
     * @param url url to load
     * @return the json data returned
     * @throws IOException on network errors
     */
    public static JSONObject get(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return new JSONObject(readAll(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))));
        }
    }

    /**
     * Performs a PUT to an url with JSON data
     *
     * @param url    url to put to
     * @param object the json data to put
     * @return the response status code
     * @throws IOException on network errors
     */
    public static int put(String url, JSONObject object) throws IOException {
        return send(url, "PUT", object);
    }


    /**
     * Performs a POST to an url with JSON data
     *
     * @param url    url to post to
     * @param object the json data to post
     * @return the response status code
     * @throws IOException on network errors
     */
    public static int post(String url, JSONObject object) throws IOException {
        return send(url, "POST", object);
    }


    /**
     * Performs a DELETE to an url
     *
     * @param url url to delete
     * @return the response status code
     * @throws IOException on network errors
     */
    public static int delete(String url) throws IOException {
        return send(url, "DELETE", null);
    }

    /* ------------------------- utils ------------------------- */

    private static int send(String url, String method, JSONObject body) throws IOException {
        // performs a send to an url

        // prepare connection
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        http.setRequestMethod(method);
        if (body != null) {
            // append body
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");
            http.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        // get
        int responseCode = http.getResponseCode();
        System.out.println(responseCode + ": " + http.getResponseMessage());

        // end
        http.disconnect();
        return responseCode;
    }

    private static String readAll(Reader rd) throws IOException {
        // converts a reader to a string
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
