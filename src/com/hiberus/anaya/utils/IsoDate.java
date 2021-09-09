package com.hiberus.anaya.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class IsoDate {
    static private SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

    static public String format(Calendar calendar) {
        //return DateTimeFormatter.ISO_LOCAL_DATE.format(calendar.toInstant());
        return isoFormat.format(calendar.getTime());
    }
}
