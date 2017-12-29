package com.bchay.wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bchay.wallpaper.database.Image;
import com.bchay.wallpaper.database.ImageDatabaseInstance;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class ImageProcessingUtility {

    //number parameter is used to determine value of screenSpan - single/multi
    static ArrayList<Image> findImages(int number, String query) {
        ArrayList<Image> images = new ArrayList<>();

        for(int i = 0; i < number; i++) {
            images.add(getRandomImage(query));
        }

        return images;
    }

    //query is a value of screenSpan:  "Span All Screens" or "Span One Screen"
    private static Image getRandomImage(String query) {
        List<Image> images;
        images = ImageDatabaseInstance.getImagesByScreenSpan(query);
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

    static ArrayList<Bitmap> imagesToBitmaps(ArrayList<Image> images, Context context) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        for(Image image : images) {
            bitmaps.add(resizeImage(image, context));
        }

        return bitmaps;
    }

    static Bitmap mergeBitmaps(ArrayList<Bitmap> bitmaps, Context context) {
        int width = bitmaps.get(0).getWidth() * bitmaps.size(); //Each bitmap has the same width, which is equal to the width of a single home screen
        int height = WallpaperManager.getInstance(context).getDesiredMinimumHeight(); //Height is the height of a home screen

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); //Draws to canvas propagate onto bitmap
        Paint paint = new Paint();

        for (int i = 0; i < bitmaps.size(); i++) {
            canvas.drawBitmap(bitmaps.get(i), i * bitmaps.get(i).getWidth(), (height - bitmaps.get(i).getHeight()) / 2, paint); //Horizontally add each bitmap to canvas
        }

        return bitmap;
    }

    static void saveImageData(Image image, Context context) {
        saveImageData(new ArrayList<Image>(Collections.singletonList(image)), context);
    }

    static void saveImageData(ArrayList<Image> images, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.hprobotics.wallpaper.WALLPAPER_PREFERENCES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        ArrayList<String> uris = new ArrayList<>();
        ArrayList<String> cropTypes = new ArrayList<>();
        ArrayList<String> screenSpans = new ArrayList<>();

        for(Image image : images) {
            uris.add(image.uri.toString());
            cropTypes.add(image.cropType);
            screenSpans.add(image.screenSpan);
        }

        editor.putStringSet("uris", new HashSet<>(uris));
        editor.putStringSet("cropTypes", new HashSet<>(cropTypes));
        editor.putStringSet("screenSpans", new HashSet<>(screenSpans));

        editor.commit();
    }

    static ArrayList<Image> retrieveSavedImageData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.hprobotics.wallpaper.WALLPAPER_PREFERENCES", Context.MODE_PRIVATE);
        ArrayList<String> uris = new ArrayList<>(sharedPreferences.getStringSet("uris", new HashSet<String>()));
        ArrayList<String> cropTypes = new ArrayList<>(sharedPreferences.getStringSet("cropTypes", new HashSet<String>()));
        ArrayList<String> screenSpans = new ArrayList<>(sharedPreferences.getStringSet("screenSpans", new HashSet<String>()));

        ArrayList<Image> images = new ArrayList<>();

        for(int i = 0; i < uris.size(); i++) {
            images.add(new Image(Uri.parse(uris.get(i)), cropTypes.get(i), screenSpans.get(i)));
        }

        return images;
    }

    static void storeXStep(float xStep, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.hprobotics.wallpaper.WALLPAPER_PREFERENCES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putFloat("xStep", xStep);
        editor.commit();
    }

    static float retrieveXStep(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.hprobotics.wallpaper.WALLPAPER_PREFERENCES", Context.MODE_PRIVATE);
        return sharedPreferences.getFloat("xStep", 1);
    }

    static Bitmap handleXStepChange(float xStep, Context context) {
            ArrayList<Image> images = retrieveSavedImageData(context);

            int numHomeScreens = (int) (1 / xStep) + 1;

            //Remove elements from images so that its length equals the number of home screens

            if (numHomeScreens < images.size()) {
                //Removes images so size() matches the number of home screens
                while(images.size() > numHomeScreens) {
                    images.remove(images.size() - 1);
                }
            }
            else images.addAll(findImages(numHomeScreens - images.size(), "Span One Screen")); //Add image to images list

            Bitmap bitmap = ImageProcessingUtility.mergeBitmaps(imagesToBitmaps(images, context), context);
            ImageProcessingUtility.saveImageData(images, context);
            return bitmap;
    }
}
