// MediaPlayerService.java
package com.example.beatify;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MediaPlayerService extends Service {

    private MediaPlayer mediaPlayer;
    private ArrayList<Song> songs;
    private int currentIndex = 0;
    public boolean isPaused = false;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setSongList(ArrayList<Song> songList) {
        this.songs = songList;
    }

    public void playSong(int index) {
        currentIndex = index;

        if (mediaPlayer != null) {
            mediaPlayer.reset();
        } else {
            mediaPlayer = new MediaPlayer();
        }

        try {
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(songs.get(currentIndex).getPath()));
            mediaPlayer.setOnPreparedListener(mp -> mp.start());
            mediaPlayer.prepareAsync(); // Async prepare uwu
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPaused = false;
        }
    }

    public void next() {
        if (songs != null) {
            currentIndex = (currentIndex + 1) % songs.size();
            playSong(currentIndex);
        }
    }

    public void previous() {
        if (songs != null) {
            currentIndex = (currentIndex - 1 + songs.size()) % songs.size();
            playSong(currentIndex);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean getPausedState(){
        return isPaused;
    }
}
