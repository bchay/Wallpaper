package com.bchay.wallpaper.database;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.net.Uri;

import java.util.List;

public class ImageDatabaseInstance {
    private static ImageDatabase database;

    public static void createDatabase(Context context) {
        if(database == null) database = Room.databaseBuilder(context, ImageDatabase.class, "image-database").build();
    }

     public static void insertImage(Image... image) {
        database.imageDao().insertImages(image);
    }

    public static List<Image> getImage(Uri uri) {
        return database.imageDao().getImage(uri);
    }

    public static List<Image> getAllImages() {
        return database.imageDao().getAllImages();
    }

    public static void deleteImages(Uri uri) {
        database.imageDao().deleteImages(uri);
    }

    public static void updateImageCropType(Uri uri, String cropType) {
        database.imageDao().updateImageCropType(uri, cropType);
    }

    public static void updateImageScreenSpan(Uri uri, String screenSpan) {
        database.imageDao().updateImageScreenSpan(uri, screenSpan);
    }
}