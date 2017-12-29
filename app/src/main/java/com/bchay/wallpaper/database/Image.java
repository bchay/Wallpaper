package com.bchay.wallpaper.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.net.Uri;
import android.support.annotation.NonNull;

@Entity(tableName = "images")
public class Image {
    @PrimaryKey
    @NonNull
    public Uri uri;
    public String cropType;
    public String screenSpan;

    public Image(Uri uri, String cropType, String screenSpan) {
        this.uri = uri;
        this.cropType = cropType;
        this.screenSpan = screenSpan;
    }
}
