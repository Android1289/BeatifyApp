package com.example.beatify;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class SharedViewModel extends AndroidViewModel {
    private final MutableLiveData<ArrayList<Playlist>> playlistsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Playlist> selectedPlaylist = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    private Playlist favoritesPlaylist;
    private static final String PREFS_NAME = "BeatifyPrefs";
    private static final String KEY_PLAYLISTS = "local_playlists";
    private static final String KEY_SONG_COVERS = "local_song_covers";
    private static final String KEY_USER_PFP_PREFIX = "local_pfp_";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private FirebaseHelper firebaseHelper;
    private HashMap<String, String> songCoversMap = new HashMap<>();

    public SharedViewModel(@NonNull Application application) {
        super(application);
        firebaseHelper = new FirebaseHelper();
        loadLocalCovers();
        checkLoginState();
    }

    public void saveLocalProfileImage(String uid, String uri) {
        if (uid == null) return;
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_PFP_PREFIX + uid, uri).apply();

        User u = currentUser.getValue();
        if (u != null && u.getUid().equals(uid)) {
            u.setProfileImageUrl(uri);
            currentUser.setValue(u);
        }
    }

    public String getLocalProfileImage(String uid) {
        if (uid == null) return null;
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_PFP_PREFIX + uid, null);
    }

    private void loadLocalCovers() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String coversJson = prefs.getString(KEY_SONG_COVERS, null);
        if (coversJson != null) {
            Type type = new TypeToken<HashMap<String, String>>() {}.getType();
            songCoversMap = new Gson().fromJson(coversJson, type);
        }
    }

    public void setSongCover(String songPath, String imageUri) {
        songCoversMap.put(songPath, imageUri);
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SONG_COVERS, new Gson().toJson(songCoversMap)).apply();
    }

    public String getSongCover(String songPath) { return songCoversMap.get(songPath); }

    private void checkLoginState() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false) && firebaseHelper.getCurrentUser() != null) {
            fetchUserData();
            loadCloudPlaylists();
        } else {
            loadLocalPlaylists();
            String localPfp = getLocalProfileImage("local_guest");
            if (localPfp != null) {
                User u = new User("local", "Guest", "");
                u.setProfileImageUrl(localPfp);
                currentUser.setValue(u);
            }
        }
    }

    public void setLoggedIn(boolean loggedIn) {
        getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
        if(loggedIn) {
            fetchUserData();
            loadCloudPlaylists();
        } else {
            currentUser.setValue(null);
            loadLocalPlaylists();
        }
    }

    public boolean isUserLoggedIn() {
        return getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void fetchUserData() {
        if(firebaseHelper.getCurrentUser() == null) return;
        String uid = firebaseHelper.getCurrentUser().getUid();

        firebaseHelper.getUserProfile(uid, user -> {
            String localPfp = getLocalProfileImage(uid);
            if (localPfp != null && user != null) {
                user.setProfileImageUrl(localPfp);
            }
            currentUser.setValue(user);
        });
    }

    private void loadCloudPlaylists() {
        if (firebaseHelper.getCurrentUser() == null) return;
        firebaseHelper.getUserPlaylists(firebaseHelper.getCurrentUser().getUid(), list -> {
            ensureFavorites(list);
            sortPlaylists(list);
            playlistsLiveData.setValue(list);
        });
    }

    private void loadLocalPlaylists() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String plistJson = prefs.getString(KEY_PLAYLISTS, null);
        ArrayList<Playlist> list;
        if (plistJson != null) {
            Type type = new TypeToken<ArrayList<Playlist>>() {}.getType();
            list = new Gson().fromJson(plistJson, type);
        } else {
            list = new ArrayList<>();
        }
        ensureFavorites(list);
        sortPlaylists(list);
        playlistsLiveData.setValue(list);
    }

    private void ensureFavorites(ArrayList<Playlist> list) {
        boolean hasFav = false;
        for(Playlist p : list) if("Favorites".equals(p.getName())) { favoritesPlaylist = p; favoritesPlaylist.setPinned(true); hasFav = true; break; }
        if(!hasFav) {
            favoritesPlaylist = new Playlist("Favorites", null, "local");
            favoritesPlaylist.setPinned(true);
            list.add(0, favoritesPlaylist);
        }
    }

    public void addPlaylist(String name, String image) {
        ArrayList<Playlist> list = playlistsLiveData.getValue();
        if(list == null) list = new ArrayList<>();
        Playlist newPl = new Playlist(name, image, isUserLoggedIn() ? firebaseHelper.getCurrentUser().getUid() : "local");
        list.add(newPl);
        sortPlaylists(list);
        playlistsLiveData.setValue(list);
        persistPlaylists();

        if(isUserLoggedIn()) firebaseHelper.savePlaylist(newPl);
    }

    public void deletePlaylist(Playlist p) {
        if(p == favoritesPlaylist) return;
        ArrayList<Playlist> list = playlistsLiveData.getValue();
        if(list != null) {
            list.remove(p);
            playlistsLiveData.setValue(list);
            persistPlaylists();
            if(isUserLoggedIn()) firebaseHelper.deletePlaylist(p);
        }
    }

    public void togglePin(Playlist p) {
        if(p == favoritesPlaylist) return;
        p.setPinned(!p.isPinned());
        sortPlaylists(playlistsLiveData.getValue());
        playlistsLiveData.setValue(playlistsLiveData.getValue());
        persistPlaylists();
        if(isUserLoggedIn()) firebaseHelper.savePlaylist(p);
    }

    public void editPlaylist(Playlist p, String newName, String newImage) {
        p.setName(newName); if(newImage!=null) p.setImageUrl(newImage);
        sortPlaylists(playlistsLiveData.getValue());
        playlistsLiveData.setValue(playlistsLiveData.getValue());
        persistPlaylists();
        if(isUserLoggedIn()) firebaseHelper.savePlaylist(p);
    }

    public void addToFavorites(Song song) {
        for(Song s : favoritesPlaylist.getSongs()) if(s.getPath().equals(song.getPath())) return;
        favoritesPlaylist.addSong(song);
        playlistsLiveData.setValue(playlistsLiveData.getValue());
        persistPlaylists();
        if(isUserLoggedIn()) firebaseHelper.savePlaylist(favoritesPlaylist);
    }

    public void removeFromFavorites(Song song) {
        favoritesPlaylist.getSongs().removeIf(s -> s.getPath().equals(song.getPath()));
        playlistsLiveData.setValue(playlistsLiveData.getValue());
        persistPlaylists();
        if(isUserLoggedIn()) firebaseHelper.savePlaylist(favoritesPlaylist);
    }

    public void addSongToPlaylist(int idx, Song song) {
        ArrayList<Playlist> list = playlistsLiveData.getValue();
        if(list != null && idx < list.size()) {
            Playlist p = list.get(idx);

            boolean exists = false;
            for(Song s : p.getSongs()) {
                if(s.getPath().equals(song.getPath())) { exists = true; break; }
            }

            if(!exists) {
                p.addSong(song);
                playlistsLiveData.setValue(list);
                persistPlaylists();
                if (isUserLoggedIn()) firebaseHelper.savePlaylist(p);
            }
        }
    }

    public boolean addSongToActivePlaylist(Song song) {
        Playlist p = selectedPlaylist.getValue();
        if (p != null) {
            for(Song s : p.getSongs()) {
                if(s.getPath().equals(song.getPath())) return false;
            }
            p.addSong(song);

            selectedPlaylist.setValue(p);

            ArrayList<Playlist> all = playlistsLiveData.getValue();
            playlistsLiveData.setValue(all);

            persistPlaylists();
            if (isUserLoggedIn()) firebaseHelper.savePlaylist(p);
            return true;
        }
        return false;
    }

    private void persistPlaylists() {
        if(!isUserLoggedIn()) {
            SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_PLAYLISTS, new Gson().toJson(playlistsLiveData.getValue())).apply();
        }
    }

    private void sortPlaylists(ArrayList<Playlist> list) {
        if (list == null) return;
        Collections.sort(list, (p1, p2) -> {
            if (p1.isPinned() && !p2.isPinned()) return -1;
            if (!p1.isPinned() && p2.isPinned()) return 1;
            return p1.getName().compareToIgnoreCase(p2.getName());
        });
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<ArrayList<Playlist>> getPlaylists() { return playlistsLiveData; }
    public LiveData<Playlist> getSelectedPlaylist() { return selectedPlaylist; }
    public void selectPlaylist(Playlist p) { selectedPlaylist.setValue(p); }
    public boolean isFavorite(Song song) {
        if(song == null || favoritesPlaylist == null) return false;
        for(Song s : favoritesPlaylist.getSongs()) if(s.getPath().equals(song.getPath())) return true;
        return false;
    }
}