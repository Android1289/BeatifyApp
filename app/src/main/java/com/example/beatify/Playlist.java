package com.example.beatify;

import java.util.ArrayList;
import java.util.UUID;

public class Playlist {
    private String id;
    private String ownerId;
    private String name;
    private String imageUrl;
    private ArrayList<Song> songs;
    private boolean isPinned;

    public Playlist() {
        if(this.songs == null) this.songs = new ArrayList<>();
    }

    public Playlist(String name, String imageUrl, String ownerId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
        this.songs = new ArrayList<>();
        this.isPinned = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public ArrayList<Song> getSongs() { return songs; }
    public void setSongs(ArrayList<Song> songs) { this.songs = songs; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public void addSong(Song song) {
        if (songs == null) songs = new ArrayList<>();
        songs.add(song);
    }

    public int getSongCount() {
        return songs == null ? 0 : songs.size();
    }
}