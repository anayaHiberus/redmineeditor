package com.hiberus.anaya.redmineeditor.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Desktop {

    /**
     * Open a page in the browser
     *
     * @param url page to open
     * @return true if it was opened, false on error
     */
    public static boolean openInBrowser(String url) {
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            try {
                java.awt.Desktop.getDesktop().browse(new URI(url));
                return true;
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
}
