package com.bchay.wallpaper;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bchay.wallpaper.database.Image;
import com.bchay.wallpaper.database.ImageDatabaseInstance;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int GET_IMAGES = 0;
    GridView gridView;
    ImageAdapter adapter;
    List<Image> images;
    ActionMode actionMode;
    List<Uri> selected = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        gridView = findViewById(R.id.photos_grid);

        ImageDatabaseInstance.createDatabase(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                images = ImageDatabaseInstance.getAllImages();
                adapter = new ImageAdapter(images, getApplicationContext());
                gridView.setAdapter(adapter);
                registerForContextMenu(gridView);
            }
        }).start();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(actionMode == null) {
                    Intent intent = new Intent(getApplicationContext(), ImageSettings.class);
                    intent.putExtra("uri", (Uri) view.getTag());
                    startActivity(intent);
                } else { //Context Action Bar is currently set
                    Uri uri = (Uri) view.getTag();
                    if(selected.contains(uri)) {
                        selected.remove(uri);
                        ((ImageView) view).setColorFilter(null);

                        if(selected.size() == 0) actionMode.finish();
                    } else {
                        selected.add(uri);
                        ((ImageView) view).setColorFilter(Color.argb(150, 255, 255, 255));
                    }

                    //Action mode is null if selected.size() == 0
                    if(actionMode != null) actionMode.setTitle(selected.size() + " Image" + (selected.size() == 1 ? "" : "s") + " Selected");
                }
            }
        });
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.home_cab, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete_cab:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for(Uri uri : selected) {
                                ImageDatabaseInstance.deleteImages(uri);
                            }
                        }
                    }).start();

                    for(Uri uri : selected) {
                        Iterator<Image> iterator = images.iterator();
                        while(iterator.hasNext()) {
                            if(iterator.next().uri.equals(uri)) {
                                iterator.remove();
                            }
                        }
                    }

                    adapter = new ImageAdapter(images, getApplicationContext());
                    gridView.setAdapter(adapter);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;

            for(int i = 0; i < gridView.getChildCount(); i++) {
                ImageView view = (ImageView) gridView.getChildAt(i);

                view.setSelected(false);
                view.setColorFilter(null);
            }

            selected = new ArrayList<>();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.navigation_home:
                return true;
            case R.id.navigation_settings:
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.navigation_delete:
                if(actionMode == null) {
                    actionMode = startActionMode(actionModeCallback);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
                    }
                }
            } else {
                images = new Image[1];
                images[0] = new Image(intent.getData(), "Stretch", "Span All Screens");
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ImageDatabaseInstance.insertImage(images);
                }
            }).start();

            for(Image image : images) {
                boolean unique = true;
                for(Image img : this.images) {
                    if(image.uri.equals(img.uri)) {
                        unique = false;
                        break;
                    }
                }

                if(unique) this.images.add(image);
            }

            adapter = new ImageAdapter(this.images, getApplicationContext());
            gridView.setAdapter(adapter);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        if(actionMode == null) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.image_context_menu, menu);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.set_as_wallpaper:
                setAsWallpaper(info.targetView);
                return true;
            case R.id.delete_image:
                deleteImage(info.targetView);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void setAsWallpaper(View view) {
        ImageView image = (ImageView) view;
        final Uri uri = (Uri) image.getTag();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Image image = ImageDatabaseInstance.getImage(uri).get(0);
                new ImageSettings().setImageAsWallpaper(image.uri, image.cropType, image.screenSpan);
            }
        }).start();
    }

    public void deleteImage(View view) {
        ImageView image = (ImageView) view;
        final Uri uri = (Uri) image.getTag();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ImageDatabaseInstance.deleteImages(uri);
            }
        }).start();


        Iterator<Image> iterator = images.listIterator();
        while(iterator.hasNext()) {
            if(iterator.next().uri.equals(uri)) {
                iterator.remove();
            }
        }

        adapter = new ImageAdapter(images, getApplicationContext());
        gridView.setAdapter(adapter);
    }
}