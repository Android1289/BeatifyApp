package com.example.beatify;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private ArrayList<Song> songs;
    private final Context context;
    private SharedViewModel viewModel;
    private OnCoverClickListener coverListener;
    private OnItemClickListener itemClickListener;
    private MainActivity mainActivity;

    public interface OnCoverClickListener {
        void onSetCoverClick(Song song);
    }

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    public void setOnCoverClickListener(OnCoverClickListener listener) {
        this.coverListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public SongAdapter(Context context, ArrayList<Song> songs) {
        this.context = context;
        this.songs = new ArrayList<>(songs);
        if (context instanceof FragmentActivity) {
            viewModel = new ViewModelProvider((FragmentActivity) context).get(SharedViewModel.class);
        }
        if (context instanceof MainActivity) {
            this.mainActivity = (MainActivity) context;
        }
    }

    public void updateList(ArrayList<Song> newList) {
        this.songs = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public ArrayList<Song> getSongs() { return songs; }
    public void refreshStateAndNotify() { notifyDataSetChanged(); }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        int themeColor = ThemeHelper.getThemeColor(context);

        holder.title.setText(song.getTitle());
        holder.title.setTextColor(themeColor);
        holder.artist.setText(song.getArtist());

        String customCover = viewModel != null ? viewModel.getSongCover(song.getPath()) : null;
        Object model = (customCover != null) ? customCover : song.getAlbumArt();

        Glide.with(context).load(model).placeholder(R.drawable.default_album_art).circleCrop().into(holder.albumArt);

        Song current = MusicPlayerManager.getInstance().getCurrentSong();
        boolean isPlaying = MusicPlayerManager.getInstance().isPlaying();

        Glide.with(context).clear(holder.playIndicator);
        if (current != null && current.getPath() != null && current.getPath().equals(song.getPath())) {
            holder.playIndicator.setVisibility(View.VISIBLE);
            holder.playIndicator.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN); // Tint playing indicator
            if (isPlaying) Glide.with(context).asGif().load(R.drawable.ic_playing_indic).into(holder.playIndicator);
            else Glide.with(context).asBitmap().load(R.drawable.ic_playing_indic).into(holder.playIndicator);
        } else {
            holder.playIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {

            if (itemClickListener != null) {
                itemClickListener.onItemClick(song);
            } else {
                MusicPlayerManager.getInstance().setQueue(songs, position);
                refreshStateAndNotify();
                 if (mainActivity != null) {
                    mainActivity.updateMiniPlayerUI();
                }
            }
        });

        holder.menuBtn.setColorFilter(themeColor);

        holder.menuBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuBtn);
            popup.inflate(R.menu.song_item_menu);

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_add_fav) {
                    if (viewModel != null) { viewModel.addToFavorites(song); Toast.makeText(context, "Added to Favorites", Toast.LENGTH_SHORT).show(); }
                    return true;
                } else if (item.getItemId() == R.id.menu_add_playlist) {
                    showAddToPlaylistDialog(song);
                    return true;
                } else if (item.getItemId() == R.id.menu_set_cover) {
                    if (coverListener != null) coverListener.onSetCoverClick(song);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void showAddToPlaylistDialog(Song song) {
        if (viewModel == null) return;
        ArrayList<Playlist> playlists = viewModel.getPlaylists().getValue();
        if (playlists == null || playlists.isEmpty()) {
            Toast.makeText(context, "No playlists available", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).getName();

        new android.app.AlertDialog.Builder(context)
                .setTitle("Add to Playlist")
                .setItems(names, (dialog, which) -> {
                    viewModel.addSongToPlaylist(which, song);
                    Toast.makeText(context, "Added to " + names[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override public int getItemCount() { return songs.size(); }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView albumArt, playIndicator;
        ImageButton menuBtn;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.songTitle);
            artist = itemView.findViewById(R.id.songArtist);
            albumArt = itemView.findViewById(R.id.musicIcon);
            playIndicator = itemView.findViewById(R.id.playingIndicator);
            menuBtn = itemView.findViewById(R.id.songMenuBtn);
        }
    }
}