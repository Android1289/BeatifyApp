package com.example.beatify;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.util.ArrayList;

public class SearchFragment extends Fragment {

    RecyclerView recycler;
    SongAdapter songAdapter;
    UserAdapter userAdapter;

    ArrayList<Song> allSongs = new ArrayList<>();
    SearchView searchView;
    MaterialButtonToggleGroup toggleGroup;
    MaterialButton btnSongs, btnUsers;

    private boolean isSearchUsers = false;
    private FirebaseHelper firebaseHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        recycler = v.findViewById(R.id.recyclerSearch);
        searchView = v.findViewById(R.id.searchView);
        toggleGroup = v.findViewById(R.id.toggleSearchType);
        btnSongs = v.findViewById(R.id.btnSearchSongs);
        btnUsers = v.findViewById(R.id.btnSearchUsers);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        firebaseHelper = new FirebaseHelper();

        allSongs = MusicLoader.getAllSongs(getContext());
        songAdapter = new SongAdapter(getContext(), allSongs);

        userAdapter = new UserAdapter();
        userAdapter.setOnUserClickListener(user -> {
            ((MainActivity)requireActivity()).showFragment(UserDetailsFragment.newInstance(user.getUid()));
        });

        recycler.setAdapter(songAdapter);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateToggleVisuals(checkedId);
                if (checkedId == R.id.btnSearchUsers) {
                    isSearchUsers = true;
                    recycler.setAdapter(userAdapter);
                } else {
                    isSearchUsers = false;
                    recycler.setAdapter(songAdapter);
                }
            }
        });

        setupSearch();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(getView(), 4, getContext());
        updateToggleVisuals(toggleGroup.getCheckedButtonId());
        if (!isSearchUsers && songAdapter != null) songAdapter.refreshStateAndNotify();
    }

    private void updateToggleVisuals(int checkedId) {
        if(btnSongs == null || btnUsers == null) return;

        int themeColor = ThemeHelper.getThemeColor(getContext());
        int white = Color.WHITE;
        int transparent = Color.TRANSPARENT;

        btnSongs.setBackgroundTintList(ColorStateList.valueOf(transparent));
        btnSongs.setTextColor(white);
        btnSongs.setStrokeColor(ColorStateList.valueOf(Color.LTGRAY));
        btnSongs.setStrokeWidth(2);

        btnUsers.setBackgroundTintList(ColorStateList.valueOf(transparent));
        btnUsers.setTextColor(white);
        btnUsers.setStrokeColor(ColorStateList.valueOf(Color.LTGRAY));
        btnUsers.setStrokeWidth(2);

       if (checkedId == R.id.btnSearchSongs) {
            btnSongs.setBackgroundTintList(ColorStateList.valueOf(themeColor));
            btnSongs.setStrokeWidth(0);
        } else if (checkedId == R.id.btnSearchUsers) {
            btnUsers.setBackgroundTintList(ColorStateList.valueOf(themeColor));
            btnUsers.setStrokeWidth(0);
        }
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { performSearch(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { performSearch(newText); return true; }
        });
    }

    private void performSearch(String q) {
        if (q == null) q = "";

        if (isSearchUsers) {
            if (q.length() > 0) {
                firebaseHelper.searchUsers(q, users -> userAdapter.updateList(users));
            } else {
                userAdapter.updateList(new ArrayList<>());
            }
        } else {
            final String qLower = q.trim().toLowerCase();
            ArrayList<Song> filtered = new ArrayList<>();
            for (Song s : allSongs) {
                if (s.getTitle().toLowerCase().contains(qLower) || s.getArtist().toLowerCase().contains(qLower)) {
                    filtered.add(s);
                }
            }
            songAdapter.updateList(filtered);
        }
    }
}