package com.bchay.wallpaper.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = {Image.class}, version = 2, exportSchema = false)
@TypeConverters(Converter.class)
abstract class ImageDatabase extends RoomDatabase {
    abstract ImageDao imageDao();
}
