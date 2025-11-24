package com.example.beatify;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LibraryFragment extends Fragment {

    RecyclerView playlistRecycler;
    PlaylistAdapter adapter;
    SharedViewModel viewModel;

    public LibraryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library, container, false);
        playlistRecycler = v.findViewById(R.id.playlistRecyclerLib);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        adapter = new PlaylistAdapter(getContext(), viewModel.getPlaylists().getValue(), new PlaylistAdapter.PlaylistActionListener() {
            @Override public void onPlaylistClick(Playlist playlist) {
                viewModel.selectPlaylist(playlist);
                ((MainActivity)requireActivity()).showFragment(new PlaylistDetailFragment());
            }
            @Override public void onPinClick(Playlist playlist) { viewModel.togglePin(playlist); }
            @Override public void onDeleteClick(Playlist playlist) { viewModel.deletePlaylist(playlist); }
            @Override public void onEditClick(Playlist playlist) { showEditDialog(playlist); }
        });

        playlistRecycler.setAdapter(adapter);
        viewModel.getPlaylists().observe(getViewLifecycleOwner(), playlists -> adapter.updateList(playlists));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(getView(), 3, getContext());
    }

    private void showEditDialog(Playlist p) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_playlist, null);
        EditText nameInput = dialogView.findViewById(R.id.playlistNameInput);
        nameInput.setText(p.getName());
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Edit Playlist").setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = nameInput.getText().toString();
                    if(!newName.isEmpty()) viewModel.editPlaylist(p, newName, null);
                }).setNegativeButton("Cancel", null).show();
    }
}