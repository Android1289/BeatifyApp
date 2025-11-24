package com.example.beatify;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

public class MusicLoader {

    public static ArrayList<Song> getAllSongs(Context context) {
        ArrayList<Song> songs = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        Cursor cursor = context.getContentResolver().query(
                uri,
                null,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

                songs.add(new Song(title, artist, data));

            } while (cursor.moveToNext());

            cursor.close();
        }

        return songs;
    }
}
