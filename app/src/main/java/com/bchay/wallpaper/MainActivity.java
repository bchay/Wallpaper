package com.bchay.wallpaper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.bchay.wallpaper.database.Image;
import com.bchay.wallpaper.database.ImageDatabaseInstance;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int GET_IMAGES = 0;
    GridLayout grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grid = findViewById(R.id.photos_grid);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);



        ImageDatabaseInstance.createDatabase(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Image> images = ImageDatabaseInstance.getAllImages();

                for(Image image : images) {
                    addImageToGrid(image.uri);
                }
            }
        }).start();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_settings:
                    return true;
            }
            return false;
        }
    };

    public void selectImage(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(intent, GET_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == GET_IMAGES && resultCode == RESULT_OK) {
             final Image[] images;

            if (intent.getClipData() != null) {
                images = new Image[intent.getClipData().getItemCount()];
                for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                    Uri uri = intent.getClipData().getItemAt(i).getUri();
                    if (uri != null) {
                        images[i] = new Image(uri, "Stretch", "Span All Screens");
                        addImageToGrid(uri);
                    }
                }
            } else {
                images = new Image[1];
                images[0] = new Image(intent.getData(), "Stretch", "Span All Screens");
                addImageToGrid(intent.getData());
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ImageDatabaseInstance.insertImage(images);
                }
            }).start();
        }
    }

    private void addImageToGrid(final Uri uri) {
        final ImageView imageView = new ImageView(this);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 450;
        params.height = 450;
        params.setMargins(10, 10, 10, 10);
        imageView.setLayoutParams(params);
        imageView.setLongClickable(true);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ImageSettings.class);
                intent.putExtra("uri", uri);
                startActivity(intent);
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //Context menu
                //Delete
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageDatabaseInstance.deleteImages(uri);
                    }
                }).start();

                grid.removeView(view);
                return true;
            }
        });

       MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                Picasso.with(MainActivity.this).load(uri).resize(450, 0).into(imageView);
                grid.addView(imageView);
            }
        });
    }
}