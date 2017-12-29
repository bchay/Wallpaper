package com.bchay.wallpaper.database;

import android.arch.persistence.room.TypeConverter;
import android.net.Uri;

public class Converter {
    @TypeConverter
    public static Uri stringToUri(String uri) {
        return uri == null ? null : Uri.parse(uri);
    }

    @TypeConverter
    public static String uriToString(Uri uri) {
        return uri == null ? null : uri.toString();
    }
}
