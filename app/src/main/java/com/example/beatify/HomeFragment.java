package com.example.beatify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    RecyclerView recycler;
    SongAdapter adapter;
    SharedViewModel viewModel;
    Song songToUpdate;

    private final ActivityResultLauncher<String> pickCover = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && songToUpdate != null) {
                    viewModel.setSongCover(songToUpdate.getPath(), uri.toString());
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Cover Updated", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        recycler = v.findViewById(R.id.recyclerHome);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        ArrayList<Song> allSongs = MusicLoader.getAllSongs(getContext());
        adapter = new SongAdapter(getContext(), allSongs);

        adapter.setOnCoverClickListener(song -> {
            songToUpdate = song;
            pickCover.launch("image/*");
        });

        recycler.setAdapter(adapter);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(getView(), 4, getContext());
        refreshAdapterAndScrollToCurrent();
    }

    public void refreshAdapterAndScrollToCurrent() {
        if (adapter != null) adapter.refreshStateAndNotify();
    }
}