package com.hiberus.anaya.redmineeditor.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * From https://stackoverflow.com/a/4308662
 */
public class JsonReader {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject fromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            return new JSONObject(readAll(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))));
        }
    }

}
