package com.bchay.wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.DisplayMetrics;

import com.bchay.wallpaper.database.Image;
import com.bchay.wallpaper.database.ImageDatabaseInstance;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.Random;

class ImageProcessingUtility {

    static Image getRandomImage() {
        List<Image> images;
        images = ImageDatabaseInstance.getAllImages();
        return images.get(new Random().nextInt(images.size()));
    }

    static Bitmap resizeImage(Image image, Context context) {
        try {
            //Determine width and height of bitmap
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int height = WallpaperManager.getInstance(context).getDesiredMinimumHeight();
            int width;
            if(image.screenSpan.equals("Span One Screen")) width = metrics.widthPixels;
            else width = WallpaperManager.getInstance(context).getDesiredMinimumWidth();

            final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            context.getContentResolver().takePersistableUriPermission(image.uri, takeFlags);

            //Crop bitmap
            switch (image.cropType) {
                case "Crop to Center":
                    return Picasso.with(context).load(image.uri).resize(width, height).centerCrop().get();
                case "Maintain Aspect Ratio": //Height may not take up entirety of screen
                    return Picasso.with(context).load(image.uri).resize(width, 0).get();
                case "Stretch":
                    return Picasso.with(context).load(image.uri).resize(width, height).get();
                default:
                    return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void saveImageData(Image image, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.hprobotics.wallpaper.WALLPAPER_PREFERENCES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("uri", image.uri.toString());
        editor.putString("cropType", image.cropType);
        editor.putString("screenSpan", image.screenSpan);

        editor.commit();
    }

    static Image retrieveSavedImageData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.hprobotics.wallpaper.WALLPAPER_PREFERENCES", Context.MODE_PRIVATE);

        return new Image(Uri.parse(sharedPreferences.getString("uri", "")), sharedPreferences.getString("cropType", ""), sharedPreferences.getString("screenSpan", ""));
    }
}