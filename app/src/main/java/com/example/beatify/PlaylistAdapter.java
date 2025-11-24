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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    public interface PlaylistActionListener {
        void onPlaylistClick(Playlist playlist);
        void onPinClick(Playlist playlist);
        void onDeleteClick(Playlist playlist);
        void onEditClick(Playlist playlist);
    }

    private ArrayList<Playlist> playlists;
    private Context context;
    private PlaylistActionListener listener;

    public PlaylistAdapter(Context context, ArrayList<Playlist> playlists, PlaylistActionListener listener) {
        this.context = context;
        this.playlists = playlists != null ? playlists : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(ArrayList<Playlist> list) {
        this.playlists = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist p = playlists.get(position);
        int themeColor = ThemeHelper.getThemeColor(context);

        holder.name.setText(p.getName());

        holder.name.setTextColor(themeColor);

        holder.count.setText(p.getSongCount() + " Songs");

        Glide.with(context)
                .load(p.getImageUrl())
                .placeholder(R.drawable.default_playlist)
                .centerCrop()
                .into(holder.cover);


        holder.menuBtn.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        holder.pinBtn.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);

        if (p.isPinned()) {
            holder.pinBtn.setImageResource(R.drawable.ic_pin_filled);
            holder.pinBtn.setAlpha(1.0f);
        } else {
            holder.pinBtn.setImageResource(R.drawable.ic_pin_outline);
            holder.pinBtn.setAlpha(0.5f);
        }

        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(p));
        holder.pinBtn.setOnClickListener(v -> listener.onPinClick(p));

        holder.menuBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuBtn);
            popup.inflate(R.menu.playlist_options_menu);


            if(p.getName().equals("Favorites")) {
                popup.getMenu().findItem(R.id.action_delete).setVisible(false);
            }

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_edit) {
                    listener.onEditClick(p);
                    return true;
                } else if (item.getItemId() == R.id.action_delete) {
                    listener.onDeleteClick(p);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, count;
        ImageView cover;
        ImageButton pinBtn, menuBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.playlistNameRow);
            count = itemView.findViewById(R.id.playlistCountRow);
            cover = itemView.findViewById(R.id.playlistCoverRow);
            pinBtn = itemView.findViewById(R.id.playlistPinBtn);
            menuBtn = itemView.findViewById(R.id.playlistMenuBtn);
        }
    }
}