package com.example.beatify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class MusicPlayerManager {
    private static MusicPlayerManager instance;
    private MediaPlayer mediaPlayer;
    private ArrayList<Song> queue = new ArrayList<>();
    private ArrayList<Song> originalQueue = new ArrayList<>();
    private int index = -1;
    private boolean isPrepared = false;
    private boolean isShuffle = false;

    private PlaybackListener listener;
    private Handler handler = new Handler();

    private Context context;
    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "beatify_music_channel";
    private static final int NOTIFICATION_ID = 1;

    private MusicPlayerManager() {}

    public static MusicPlayerManager getInstance() {
        if (instance == null) instance = new MusicPlayerManager();
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        mediaSession = new MediaSessionCompat(this.context, "BeatifyMediaSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                next();
            }

            @Override
            public void onSkipToPrevious() {
                prev();
            }
        });
        mediaSession.setActive(true);
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.listener = listener;
    }

    public PlaybackListener getPlaybackListener() {
        return listener;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void toggleShuffle() {
        isShuffle = !isShuffle;
        if (originalQueue == null || originalQueue.isEmpty()) return;

        if (isShuffle) {
            queue = new ArrayList<>(originalQueue);
            shuffleCurrentQueue();
        } else {
            Song currentSong = getCurrentSong();
            queue = new ArrayList<>(originalQueue);
            index = (currentSong != null) ? queue.indexOf(currentSong) : 0;
            if (index == -1) index = 0;
        }
        if (listener != null) listener.onPlayStateChanged(isPlaying());
        showNotification();
    }

    private void shuffleCurrentQueue() {
        if (queue.size() > 1) {
            Song current = getCurrentSong();
            if (current != null && queue.contains(current)) {
                queue.remove(current);
                Collections.shuffle(queue);
                queue.add(0, current);
                index = 0;
            } else {
                Collections.shuffle(queue);
                index = 0;
            }
        } else {
            index = 0;
        }
    }

    public void setQueue(ArrayList<Song> list, int startIndex) {
        if (list == null) return;
        originalQueue = list;

        if (isShuffle) {
            queue = new ArrayList<>(list);
            Song startSong = list.get(startIndex);
            queue.remove(startSong);
            Collections.shuffle(queue);
            queue.add(0, startSong);
            index = 0;
        } else {
            queue = list;
            index = startIndex;
        }
        playCurrent();
    }

    public void playCurrent() {
        if (queue == null || queue.size() == 0 || index < 0 || index >= queue.size()) return;
        Song s = queue.get(index);
        playPath(s.getPath());
        if (listener != null) listener.onTrackChanged(s, index);
    }

    public void playPath(String path) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            isPrepared = true;
            mediaPlayer.start();
            if (listener != null) listener.onPlayStateChanged(true);

            showNotification();

            mediaPlayer.setOnCompletionListener(mp -> {
                next();
            });
        } catch (IOException e) {
            e.printStackTrace();
            isPrepared = false;
            if (listener != null) listener.onError(e);
        }
    }

    public void playPause() {
        if (mediaPlayer == null) {
            playCurrent();
            return;
        }
        if (mediaPlayer.isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null) listener.onPlayStateChanged(false);
            showNotification();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (listener != null) listener.onPlayStateChanged(true);
            showNotification();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void next() {
        if (queue == null || queue.size() == 0) return;
        index++;
        if (index >= queue.size()) index = 0;
        playCurrent();
    }

    public void prev() {
        if (queue == null || queue.size() == 0) return;
        index--;
        if (index < 0) index = queue.size() - 1;
        playCurrent();
    }

    public @Nullable Song getCurrentSong() {
        if (queue == null || index < 0 || index >= queue.size()) return null;
        return queue.get(index);
    }

    public int getCurrentIndex() {
        return index;
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            if (listener != null) listener.onPlayStateChanged(false);
            if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Beatify Music",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls for music playback");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification() {
        if (context == null || getCurrentSong() == null) return;

        Song song = getCurrentSong();
        boolean isPlaying = isPlaying();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap albumArt = BitmapFactory.decodeResource(context.getResources(), song.getAlbumArt());

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .build());

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0, 1f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build());


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music)
                .setLargeIcon(albumArt)
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        builder.addAction(R.drawable.ic_skip_prev, "Previous",
                androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        if (isPlaying) {
            builder.addAction(R.drawable.ic_pause, "Pause",
                    androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE));
        } else {
            builder.addAction(R.drawable.ic_play, "Play",
                    androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY));
        }

        builder.addAction(R.drawable.ic_skip_next, "Next",
                androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public interface PlaybackListener {
        void onTrackChanged(Song song, int index);
        void onPlayStateChanged(boolean isPlaying);
        void onError(Exception e);
    }
}