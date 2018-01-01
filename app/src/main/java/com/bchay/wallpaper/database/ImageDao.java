package com.bchay.wallpaper.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.net.Uri;

import java.util.List;

@Dao
public interface ImageDao {
    @Query("SELECT * FROM images")
    List<Image> getAllImages();

    @Query("SELECT * FROM images WHERE uri LIKE :uri")
    List<Image> getImage(Uri... uri);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertImages(Image... images);

    @Query("DELETE FROM images WHERE uri LIKE :uri")
    void deleteImages(Uri uri);

    @Query("UPDATE images SET cropType = :cropType WHERE uri = :uri")
    void updateImageCropType(Uri uri, String cropType);

    @Query("UPDATE images SET screenSpan = :screenSpan WHERE uri = :uri")
    void updateImageScreenSpan(Uri uri, String screenSpan);
}
