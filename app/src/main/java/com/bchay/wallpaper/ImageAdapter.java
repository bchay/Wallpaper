package com.bchay.wallpaper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.bchay.wallpaper.database.Image;
import com.squareup.picasso.Picasso;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private List<Image> images;
    private Context context;

    ImageAdapter(List<Image> images, Context context) {
        this.images = images;
        this.context = context;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Object getItem(int i) {
        return images.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        final ImageView imageView;
        final Uri uri = images.get(i).uri;

        final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, takeFlags);

        if (view == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(450, 450));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(5, 5, 5, 5);
            imageView.setTag(uri);
        } else {
            imageView = (ImageView) view;
        }

        Picasso.with(context).load(uri).resize(450, 0).into(imageView);

        return imageView;
    }
}
