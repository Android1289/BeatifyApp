package com.example.beatify;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

public class MainActivity extends AppCompatActivity implements MusicPlayerManager.PlaybackListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    BottomNavigationView bottomNav;
    ShapeableImageView userIcon;
    LinearLayout topBar;

    MusicPlayerManager player;
    LinearLayout miniPlayer, miniClickableArea;
    ImageView miniArt, btnTheme;
    TextView miniTitle, miniArtist;
    ImageButton btnPrev, btnPlayPause, btnNext;

    SharedViewModel viewModel;

    MusicPlayerFragment musicPlayerFragment = new MusicPlayerFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        checkAndRequestPermissions();

        // --- INITIALIZE YOUTUBE DOWNLOADER ---
        try {
            YoutubeDL.getInstance().init(this);
        } catch (YoutubeDLException e) {
            Toast.makeText(this, "Welcome to BEATIFY", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // 1. Init Views
        bottomNav = findViewById(R.id.bottomNav);
        topBar = findViewById(R.id.topBar);

        userIcon = findViewById(R.id.userIcon);
        btnTheme = findViewById(R.id.btnTheme);

        miniPlayer = findViewById(R.id.miniPlayer);
        miniClickableArea = findViewById(R.id.miniClickableArea);
        miniArt = findViewById(R.id.miniArt);
        miniTitle = findViewById(R.id.miniTitle);
        miniArtist = findViewById(R.id.miniArtist);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);

        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        userIcon.setOnClickListener(v -> showFragment(new UserDetailsFragment()));

        btnTheme.setOnClickListener(v -> showThemePickerDialog());

        player = MusicPlayerManager.getInstance();
        player.init(getApplicationContext());
        player.setPlaybackListener(this);

        btnPrev.setOnClickListener(v -> { player.prev(); updateMiniPlayerUI(); });
        btnNext.setOnClickListener(v -> { player.next(); updateMiniPlayerUI(); });
        btnPlayPause.setOnClickListener(v -> {
            player.playPause();
            updatePlayPauseIcon();
            notifyListsToRefresh();
            updateMiniPlayerUI();
        });
        miniClickableArea.setOnClickListener(v -> openFullPlayer());

        showFragment(new HomeFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) showFragment(new HomeFragment());
            else if (id == R.id.nav_search) showFragment(new SearchFragment());
            else if (id == R.id.nav_library) showFragment(new LibraryFragment());
            else if (id == R.id.nav_create) showFragment(new CreateFragment());
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        getSupportFragmentManager().addOnBackStackChangedListener(this::updateMiniPlayerUI);
        updateMiniPlayerUI();
        updateUITheme();

        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(userIcon);
            } else {
                userIcon.setImageResource(R.drawable.ic_user_placeholder);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayerUI();
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finish();
                startActivity(getIntent());
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                Toast.makeText(this, "Permissions Denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void updateUITheme() {
        ThemeHelper.applyTheme(topBar, 1, this);
        ThemeHelper.applyTheme(miniPlayer, 2, this);
        ThemeHelper.applyTheme(bottomNav, 1, this);

        ColorStateList navColors = ThemeHelper.createBottomNavColorStateList(this);
        bottomNav.setItemIconTintList(navColors);
        bottomNav.setItemTextColor(navColors);

        int themeColor = ThemeHelper.getThemeColor(this);
        int darkerShade = ThemeHelper.getDarkerColor(themeColor);
        bottomNav.setItemActiveIndicatorColor(ColorStateList.valueOf(darkerShade));

        Fragment current = getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
        if (current != null && current.getView() != null) {
            if(current instanceof HomeFragment || current instanceof SearchFragment || current instanceof LibraryFragment) {
                ThemeHelper.applyTheme(current.getView(), 1, this);
            } else {
                ThemeHelper.applyTheme(current.getView(), 2, this);
            }
        }
    }

    private void showThemePickerDialog() {
        List<Integer> colors = Arrays.asList(
                Color.parseColor("#BB86FC"), Color.parseColor("#03DAC5"),
                Color.parseColor("#FF5252"), Color.parseColor("#4CAF50"),
                Color.parseColor("#FF9800"), Color.parseColor("#2196F3")
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Theme Color");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        for (int c : colors) {
            View v = new View(this);
            v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120));
            v.setBackgroundColor(c);
            ((LinearLayout.LayoutParams)v.getLayoutParams()).setMargins(0, 10, 0, 10);
            v.setOnClickListener(view -> {
                ThemeHelper.saveThemeColor(this, c);
                updateUITheme();
                finish();
                startActivity(getIntent());
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
            layout.addView(v);
        }
        builder.setView(layout);
        builder.show();
    }

    public void showFragment(Fragment fragment) {
        boolean isDetail = fragment instanceof PlaylistDetailFragment || fragment instanceof UserDetailsFragment;
        if (!isDetail && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (isDetail) {
            ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down);
            ft.replace(R.id.mainFragmentContainer, fragment);
            ft.addToBackStack(null);
        } else {
            if (!fragment.isAdded()) ft.add(R.id.mainFragmentContainer, fragment);
            ft.replace(R.id.mainFragmentContainer, fragment);
        }
        ft.commit();
        notifyListsToRefresh();
        updateMiniPlayerUI();
    }

    private void openFullPlayer() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down);
        ft.replace(R.id.mainFragmentContainer, musicPlayerFragment);
        ft.addToBackStack("Player");
        ft.commit();
        miniPlayer.setVisibility(View.GONE);
    }

    public void updateMiniPlayerUI() {
        Song current = player.getCurrentSong();
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
        boolean isPlayer = (top instanceof MusicPlayerFragment);

        if (current != null && !isPlayer) {
            miniPlayer.setVisibility(View.VISIBLE);
            miniTitle.setText(current.getTitle());
            miniArtist.setText(current.getArtist());
            String customCover = viewModel.getSongCover(current.getPath());
            Object model = (customCover != null) ? customCover : current.getAlbumArt();

            Glide.with(this).load(model).placeholder(R.drawable.ic_music).centerCrop().into(miniArt);
            updatePlayPauseIcon();
        } else {
            miniPlayer.setVisibility(View.GONE);
        }
        notifyListsToRefresh();
    }

    private void updatePlayPauseIcon() {
        btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void notifyListsToRefresh() {
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
        if (top instanceof HomeFragment) ((HomeFragment) top).refreshAdapterAndScrollToCurrent();
    }

    @Override public void onTrackChanged(Song song, int index) { updateMiniPlayerUI(); }
    @Override public void onPlayStateChanged(boolean isPlaying) { updateMiniPlayerUI(); }
    @Override public void onError(Exception e) {}
}