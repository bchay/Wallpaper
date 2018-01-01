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
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;
import com.bchay.wallpaper.database.Image;
import com.bchay.wallpaper.database.ImageDatabaseInstance;

public class LiveWallpaperService extends WallpaperService {
    Bitmap bitmap;
    String screenSpanMode;
    int xPixelsOffset = 0;



    @Override
    public Engine onCreateEngine() {
        //Receive broadcast from activity - includes uri information
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.bchay.wallpaper.SET_WALLPAPER");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        return new WallpaperEngine();
    }

    //Modified from https://stackoverflow.com/a/26080415
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();

            if(action != null && action.equals("com.bchay.wallpaper.SET_WALLPAPER")) {
                final Uri uri = intent.getParcelableExtra("uri");
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);

                final String cropType = intent.getStringExtra("cropType");
                screenSpanMode = intent.getStringExtra("screenSpan");

                new Thread(new Runnable() {
                    public void run() {
                        bitmap = ImageProcessingUtility.resizeImage(new Image(uri, cropType, screenSpanMode), context);
                    }
                }).start();
            }
        }
    };

    private class WallpaperEngine extends Engine {
        private boolean visible = true;
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };

        final Handler changeWallpaperHandler = new Handler();
        Runnable changeWallpaperRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bitmap = ImageProcessingUtility.resizeImage(ImageProcessingUtility.getRandomImage(), getApplicationContext());
                        draw();
                    }
                }).start();

                changeWallpaperHandler.postDelayed(changeWallpaperRunnable, (long) (Double.valueOf(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("delay", "")) * 1000));
            }
        };

        WallpaperEngine() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ImageDatabaseInstance.createDatabase(getApplicationContext());
                    Image image = ImageProcessingUtility.retrieveSavedImageData(getApplicationContext());
                    screenSpanMode = image.screenSpan;

                    bitmap = ImageProcessingUtility.resizeImage(image, getApplicationContext());
                    handler.post(drawRunner);
                }
            }).start();

            changeWallpaperHandler.postDelayed(changeWallpaperRunnable, (long) (Double.valueOf(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("delay", "")) * 1000));
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
            changeWallpaperHandler.removeCallbacks(changeWallpaperRunnable);

        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            draw();
        }

        private void draw() {
            SurfaceHolder surfaceHolder = getSurfaceHolder();

            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

                    if(bitmap != null) {
                        if(screenSpanMode != null && screenSpanMode.equals("Span All Screens")) canvas.drawBitmap(bitmap, new Rect(xPixelsOffset, 0, xPixelsOffset + canvas.getWidth(), canvas.getHeight()), new Rect(0, (canvas.getHeight() - bitmap.getHeight()) / 2, canvas.getWidth(), canvas.getHeight()), paint);
                        else canvas.drawBitmap(bitmap, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), new Rect(0, (canvas.getHeight() - bitmap.getHeight()) / 2, canvas.getWidth(), canvas.getHeight()), paint);
                    }
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            handler.removeCallbacks(drawRunner);
        }

        public void onOffsetsChanged(float xOffset, float yOffset, final float xStep, float yStep, final int xPixels, int yPixels) {
            xPixelsOffset = -xPixels;
            draw();
        }
    }
}