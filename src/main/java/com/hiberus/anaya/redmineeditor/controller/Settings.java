package com.hiberus.anaya.redmineeditor.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Global settings
 */
public class Settings {
    private static final Map<String, String> data = new HashMap<>();

    public enum ENTRY {
        /**
         * Redmine url
         */
        URL,
        /**
         * Redmine key
         */
        KEY,
    }

    /* ------------------------- public ------------------------- */

    /**
     * Returns the setting entry
     *
     * @param entry entry to return
     * @return that entry value
     */
    public static String get(ENTRY entry) {
        if (data.containsKey(entry.name())) {
            // contains
            return data.get(entry.name());
        } else {
            // doesn't contains
            System.err.println("No configuration with entry " + entry.name() + " is present.");
            return ""; // TODO: better defaults
        }
    }

    /* ------------------------- load on init ------------------------- */

    static {
        // Load settings from hardcoded file
        String filename = "/home/anaya/abel/personal/proyectos/redmine/settings.conf";

        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            lines
                    .map(line -> line.replaceAll("#.*", "")) // remove comments
                    .filter(s -> !s.isBlank()) // skip empty
                    .forEach(line -> {
                        String[] parts = line.split("=");
                        data.put(parts[0], parts[1]);
                    });
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Settings file error!");
        }
    }

}
