package com.bchay.wallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.bchay.wallpaper.database.Image;
import com.bchay.wallpaper.database.ImageDatabaseInstance;
import java.util.ArrayList;

public class LiveWallpaperService extends WallpaperService {
    Uri uri;
    Bitmap bitmap;
    String screenSpanMode;
    float xStepOffset = 1;
    int xPixelsOffset = 0;

    @Override
    public Engine onCreateEngine() {
        //Receive broadcast from activity - includes uri information
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.hprobotics.wallpaper.SET_WALLPAPER");
        //LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        return new WallpaperEngine();
    }

    //Modified from https://stackoverflow.com/a/26080415
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action != null && action.equals("com.example.hprobotics.wallpaper.SET_WALLPAPER")) {
                uri = intent.getParcelableExtra("uri");
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);

                final String cropType = intent.getStringExtra("cropType");
                screenSpanMode = intent.getStringExtra("screenSpan");
                if(screenSpanMode.equals("Span All Screens")) {
                    new Thread(new Runnable() {
                        public void run() {
                            bitmap = ImageProcessingUtility.resizeImage(new Image(uri, cropType, screenSpanMode), getApplicationContext());
                            ImageProcessingUtility.saveImageData(new Image(uri, cropType, screenSpanMode), getApplicationContext());
                        }
                    }).start();
                } else {
                    //Bitmap will be set by draw method; needs wallpaper offset information
                    bitmap = null;
                }
            }
        }
    };

    private class WallpaperEngine extends Engine {
        private boolean visible = true;
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw(xStepOffset, xPixelsOffset);
            }
        };

        WallpaperEngine() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ImageDatabaseInstance.createDatabase(getApplicationContext());
                    ArrayList<Image> images = ImageProcessingUtility.retrieveSavedImageData(getApplicationContext());
                    screenSpanMode = images.get(0).screenSpan;
                    uri = images.get(0).uri;

                    if(images.size() <= 1) bitmap = ImageProcessingUtility.resizeImage(images.get(0), getApplicationContext());
                    else bitmap = ImageProcessingUtility.mergeBitmaps(ImageProcessingUtility.imagesToBitmaps(images, getApplicationContext()), getApplicationContext());
                    handler.post(drawRunner);

                    float xStep = ImageProcessingUtility.retrieveXStep(getApplicationContext());

                    //Log.i("BITMAP", "xStep: " + xStep + "images size: " + images.size());
                    if(xStep != 0 && images.get(0).screenSpan.equals("Span One Screen") && images.size() <= 1) { //Need to randomly select other images
                        setBitmapSingleScreen(xStep);
                        draw(xStepOffset, xPixelsOffset);
                    }
                }
            }).start();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            draw(xStepOffset, xPixelsOffset);
        }

        private void draw(final float xStep, int xPixels) {
            SurfaceHolder surfaceHolder = getSurfaceHolder();

            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

                    if(bitmap != null) {
                        int xPos = (int) (xPixels / xStep);
                        if(screenSpanMode != null && screenSpanMode.equals("Span All Screens")) canvas.drawBitmap(bitmap, new Rect(xPixels, 0, xPixels + canvas.getWidth(), canvas.getHeight()), new Rect(0, (canvas.getHeight() - bitmap.getHeight()) / 2, canvas.getWidth(), canvas.getHeight()), paint);
                        else {
                            canvas.drawBitmap(bitmap, new Rect(xPos, 0, xPos + canvas.getWidth(), canvas.getHeight()), new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), paint);
                        }
                    }
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            handler.removeCallbacks(drawRunner);
            if (visible) {
                handler.postDelayed(drawRunner, 100);
            }
        }

        public void onOffsetsChanged(float xOffset, float yOffset, final float xStep, float yStep, final int xPixels, int yPixels) {
            if(xStepOffset != xStep) {
                if(xStep != 0) {
                    ImageProcessingUtility.storeXStep(xStep, getApplicationContext());

                    if(screenSpanMode.equals("Span One Screen")) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                bitmap = ImageProcessingUtility.handleXStepChange(xStep, getApplicationContext());
                            }
                        }).start();
                    }
                }
            }

            xStepOffset = xStep;
            xPixelsOffset = -xPixels;
            draw(xStep, -xPixels);
        }
    }

    //Finds n bitmaps, where n is the number of home screens a user has
    //Each returned bitmap has a screenSpan of "Span One Screen"
    private void setBitmapSingleScreen(float xStep) {
        int numHomeScreens = (int) (1 / xStep) + 1;

        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        ArrayList<Image> images = new ArrayList<>();
        images.add(ImageDatabaseInstance.getImage(uri).get(0));

        images.addAll(ImageProcessingUtility.findImages(numHomeScreens - 1, screenSpanMode));
        bitmaps.addAll(ImageProcessingUtility.imagesToBitmaps(images, getApplicationContext()));
        bitmap = ImageProcessingUtility.mergeBitmaps(bitmaps, getApplicationContext());

        ImageProcessingUtility.saveImageData(images, getApplicationContext());
    }
}