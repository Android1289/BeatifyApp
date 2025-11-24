package com.example.beatify;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

public class PlaylistDetailFragment extends Fragment implements MusicPlayerManager.PlaybackListener {

    private SharedViewModel viewModel;
    private Playlist currentPlaylist;
    private SongAdapter adapter;

    private ImageView cover;
    private TextView name, count;
    private RecyclerView recycler;
    private Toolbar toolbar;
    private FloatingActionButton fabPlay;
    private FloatingActionButton fabAdd;
    private ImageButton btnShuffle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playlist_detail, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        cover = v.findViewById(R.id.detailCover);
        name = v.findViewById(R.id.detailName);
        count = v.findViewById(R.id.detailCount);
        recycler = v.findViewById(R.id.detailRecycler);
        toolbar = v.findViewById(R.id.detailToolbar);

        fabPlay = v.findViewById(R.id.btnPlayPlaylist);
        fabAdd = v.findViewById(R.id.btnAddSongs);
        btnShuffle = v.findViewById(R.id.btnShuffle);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        toolbar.setNavigationOnClickListener(view -> requireActivity().getSupportFragmentManager().popBackStack());

        toolbar.setOnMenuItemClickListener(item -> {
            if (currentPlaylist == null) return false;
            if (item.getItemId() == R.id.menu_edit_pl) { showEditDialog(); return true; }
            else if (item.getItemId() == R.id.menu_delete_pl) {
                if(currentPlaylist.getName().equals("Favorites")) Toast.makeText(getContext(), "Cannot delete Favorites", Toast.LENGTH_SHORT).show();
                else {
                    new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Playlist").setMessage("Are you sure?")
                            .setPositiveButton("Delete", (d,w) -> { viewModel.deletePlaylist(currentPlaylist); requireActivity().getSupportFragmentManager().popBackStack(); })
                            .setNegativeButton("Cancel", null).show();
                }
                return true;
            }
            return false;
        });

        viewModel.getSelectedPlaylist().observe(getViewLifecycleOwner(), playlist -> {
            this.currentPlaylist = playlist;
            if (playlist != null) {
                name.setText(playlist.getName());
                count.setText(playlist.getSongCount() + " Songs");
                Glide.with(this).load(playlist.getImageUrl()).placeholder(R.drawable.default_playlist).centerCrop().into(cover);

                adapter = new SongAdapter(getContext(), playlist.getSongs());
                recycler.setAdapter(adapter);

                fabPlay.setOnClickListener(view -> {
                    if (playlist.getSongs().size() > 0) MusicPlayerManager.getInstance().setQueue(playlist.getSongs(), 0);
                });
            }
        });

        btnShuffle.setOnClickListener(v2 -> {
            MusicPlayerManager.getInstance().toggleShuffle();
            updateShuffleIcon();
            Toast.makeText(getContext(), MusicPlayerManager.getInstance().isShuffle() ? "Shuffle On" : "Shuffle Off", Toast.LENGTH_SHORT).show();
        });

        fabAdd.setOnClickListener(view -> showAddSongsDialog());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(getView(), 1, getContext());
        updateShuffleIcon();


        MusicPlayerManager.getInstance().setPlaybackListener(this);
        if (adapter != null) adapter.refreshStateAndNotify();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (MusicPlayerManager.getInstance().getPlaybackListener() == this) {
            MusicPlayerManager.getInstance().setPlaybackListener(null);
        }
    }


    private void updateShuffleIcon() {
        if (btnShuffle == null || getContext() == null) return;
        boolean isShuffle = MusicPlayerManager.getInstance().isShuffle();

        btnShuffle.setImageResource(R.drawable.ic_shuffle);


        if (isShuffle) {
            int themeColor = ThemeHelper.getThemeColor(getContext());
            btnShuffle.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
            btnShuffle.setAlpha(1.0f);
        } else {

            btnShuffle.setColorFilter(null);
            btnShuffle.setAlpha(0.5f);
        }
    }


    private void showEditDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_playlist, null);
        EditText nameInput = dialogView.findViewById(R.id.playlistNameInput);
        nameInput.setText(currentPlaylist.getName());
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Edit Playlist").setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = nameInput.getText().toString();
                    if(!newName.isEmpty()) viewModel.editPlaylist(currentPlaylist, newName, null);
                }).setNegativeButton("Cancel", null).show();
    }


    private void showAddSongsDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_search, null);


        ThemeHelper.applyTheme(dialogView, 4, getContext());


        dialogView.findViewById(R.id.toggleSearchType).setVisibility(View.GONE);

        RecyclerView searchRecycler = dialogView.findViewById(R.id.recyclerSearch);
        SearchView searchView = dialogView.findViewById(R.id.searchView);
        searchRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        ArrayList<Song> allSongs = MusicLoader.getAllSongs(getContext());
        SongAdapter searchAdapter = new SongAdapter(getContext(), allSongs);


        searchAdapter.setOnItemClickListener(song -> {
            boolean success = viewModel.addSongToActivePlaylist(song);
            if (success) Toast.makeText(getContext(), "Added " + song.getTitle(), Toast.LENGTH_SHORT).show();
            else Toast.makeText(getContext(), "Song already in playlist", Toast.LENGTH_SHORT).show();
        });

        searchRecycler.setAdapter(searchAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { filterSongs(query, allSongs, searchAdapter); return true; }
            @Override
            public boolean onQueryTextChange(String newText) { filterSongs(newText, allSongs, searchAdapter); return true; }
        });

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Add Songs")
                .setView(dialogView)
                .setPositiveButton("Done", null)
                .show();
    }

    private void filterSongs(String q, ArrayList<Song> all, SongAdapter adapter) {
        String lower = q.toLowerCase();
        ArrayList<Song> filtered = new ArrayList<>();
        for(Song s : all) {
            if(s.getTitle().toLowerCase().contains(lower) || s.getArtist().toLowerCase().contains(lower))
                filtered.add(s);
        }
        adapter.updateList(filtered);
    }


    @Override
    public void onTrackChanged(Song song, int index) {
        if (adapter != null) {
            adapter.refreshStateAndNotify();
        }
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        if (adapter != null) {
            adapter.refreshStateAndNotify();
        }
        updateShuffleIcon();
    }

    @Override
    public void onError(Exception e) {
    }
}