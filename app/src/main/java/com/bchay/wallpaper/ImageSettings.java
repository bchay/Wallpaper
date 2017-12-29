package com.bchay.wallpaper;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.bchay.wallpaper.database.Image;
import com.bchay.wallpaper.database.ImageDatabaseInstance;
import com.squareup.picasso.Picasso;

public class ImageSettings extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Uri uri;
    ImageView imageView;
    Spinner cropType;
    Spinner screenSpan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_settings);

        cropType = findViewById(R.id.crop_type_spinner);
        screenSpan = findViewById(R.id.screen_span_spinner);

        final ArrayAdapter<CharSequence> cropTypeAdapter = ArrayAdapter.createFromResource(this, R.array.crop_type, android.R.layout.simple_spinner_item);
        final ArrayAdapter<CharSequence> screenSpanAdapter = ArrayAdapter.createFromResource(this, R.array.screen_span, android.R.layout.simple_spinner_item);

        cropTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        screenSpanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cropType.setAdapter(cropTypeAdapter);
        screenSpan.setAdapter(screenSpanAdapter);

        cropType.setOnItemSelectedListener(this);
        screenSpan.setOnItemSelectedListener(this);

        Intent intent = getIntent();

        uri = intent.getParcelableExtra("uri");

        final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(uri, takeFlags);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Image image = ImageDatabaseInstance.getImage(uri).get(0);

                cropType.setSelection(cropTypeAdapter.getPosition(image.cropType));
                screenSpan.setSelection(screenSpanAdapter.getPosition(image.screenSpan));
            }
        }).start();

        imageView = findViewById(R.id.image_view);
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        Picasso.with(this).load(uri).resize(metrics.widthPixels, 0).into(imageView);
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, View view, final int position, long id) {
        switch (parent.getId()) {
            case R.id.crop_type_spinner:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageDatabaseInstance.updateImageCropType(uri, parent.getItemAtPosition(position).toString());
                    }
                }).start();
                break;
            case R.id.screen_span_spinner:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageDatabaseInstance.updateImageScreenSpan(uri, parent.getItemAtPosition(position).toString());
                    }
                }).start();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void setWallpaper(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Save designated image information - for display with live wallpaper chooser
                ImageProcessingUtility.saveImageData(new Image(uri, cropType.getSelectedItem().toString(), screenSpan.getSelectedItem().toString()), getApplicationContext());

                //Start LiveWallpaperService if it has not already been started
                if(WallpaperManager.getInstance(getApplicationContext()).getWallpaperInfo() == null || !WallpaperManager.getInstance(getApplicationContext()).getWallpaperInfo().getServiceName().equals("om.example.hprobotics.wallpaper.LiveWallpaperService")) {
                    Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(getApplicationContext(), LiveWallpaperService.class));
                    startActivity(intent);
                }

                //Send broadcast to set wallpaper
                Intent intent = new Intent();
                intent.setAction("com.example.hprobotics.wallpaper.SET_WALLPAPER");
                intent.putExtra("uri", uri);
                intent.putExtra("cropType", cropType.getSelectedItem().toString());
                intent.putExtra("screenSpan", screenSpan.getSelectedItem().toString());
                //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }).start();
    }
}
