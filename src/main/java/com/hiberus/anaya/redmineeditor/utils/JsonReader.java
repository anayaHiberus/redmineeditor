package com.hiberus.anaya.redmineeditor.utils;

import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @see JsonReader#fromUrl(String)
 * From https://stackoverflow.com/a/4308662
 */
public class JsonReader {

    /**
     * Reads JSON data from an url
     *
     * @param url url to load
     * @return the json data returned
     * @throws IOException on network errors
     */
    public static JSONObject fromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return new JSONObject(readAll(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))));
        }
    }

    // from https://www.baeldung.com/java-convert-reader-to-string
    private static String readAll(Reader rd) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = rd.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        return buffer.toString();
    }

}
