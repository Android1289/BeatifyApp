package com.example.beatify;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import java.util.concurrent.TimeUnit;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


public class MusicPlayerFragment extends Fragment implements MusicPlayerManager.PlaybackListener {

    private ImageView albumArt, btnCollapse;
    private TextView title, artist, currentTime, totalTime;
    private SeekBar seekBar;
    private ImageButton btnPrev, btnPlayPause, btnNext, btnFavorite, btnEqualizer;
    private View pulseBackground;
    private Animation pulseAnimation;
    private MusicPlayerManager player;
    private SharedViewModel viewModel;
    private Handler handler = new Handler();

    private final ActivityResultLauncher<String> pickCover = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && player.getCurrentSong() != null) {
                    viewModel.setSongCover(player.getCurrentSong().getPath(), uri.toString());
                    Glide.with(this).load(uri).into(albumArt);
                    Toast.makeText(getContext(), "Cover Updated Locally", Toast.LENGTH_SHORT).show();
                }
            });

    private Runnable updateSeek = new Runnable() {
        @Override
        public void run() {
            if (player.isPlaying() && player.getCurrentSong() != null) {
                try {
                    int pos = player.getMediaPlayer().getCurrentPosition();
                    int dur = player.getMediaPlayer().getDuration();
                    seekBar.setMax(dur);
                    seekBar.setProgress(pos);
                    currentTime.setText(formatTime(pos));
                    totalTime.setText(formatTime(dur));
                } catch (Exception e) {
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_player, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        albumArt = view.findViewById(R.id.playerAlbumArtFull);
        pulseBackground = view.findViewById(R.id.pulseBackground);
        title = view.findViewById(R.id.playerTitleFull);
        artist = view.findViewById(R.id.playerArtistFull);
        currentTime = view.findViewById(R.id.playerCurrentTimeFull);
        totalTime = view.findViewById(R.id.playerTotalTimeFull);
        seekBar = view.findViewById(R.id.playerSeekBarFull);
        btnCollapse = view.findViewById(R.id.btnCollapse);
        btnPrev = view.findViewById(R.id.btnPrevFull);
        btnPlayPause = view.findViewById(R.id.btnPlayPauseFull);
        btnNext = view.findViewById(R.id.btnNextFull);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnEqualizer = view.findViewById(R.id.btnEqualizer);

        player = MusicPlayerManager.getInstance();
        player.setPlaybackListener(this);

        if (getContext() != null) {
            pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse);
        }


        btnPrev.setOnClickListener(v -> player.prev());
        btnNext.setOnClickListener(v -> player.next());
        btnPlayPause.setOnClickListener(v -> { player.playPause(); updatePlayPause(); });
        btnCollapse.setOnClickListener(v -> { if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack(); });

        btnEqualizer.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.getMediaPlayer().getAudioSessionId());
                intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                startActivityForResult(intent, 0);
            } catch (Exception e) {
                Toast.makeText(getContext(), "No Equalizer found on device", Toast.LENGTH_SHORT).show();
            }
        });

        albumArt.setOnLongClickListener(v -> {
            pickCover.launch("image/*");
            return true;
        });

        btnFavorite.setOnClickListener(v -> {
            Song current = player.getCurrentSong();
            if (current != null) {
                if (viewModel.isFavorite(current)) viewModel.removeFromFavorites(current);
                else viewModel.addToFavorites(current);
                updateFavoriteIcon(current);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player.getMediaPlayer() != null) player.getMediaPlayer().seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateUI();

        handler.post(updateSeek);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(getView(), 3, getContext());
        int themeColor = ThemeHelper.getThemeColor(getContext());
        seekBar.getThumb().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        seekBar.getProgressDrawable().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);

        updateUI();
    }

    private void updateUI() {
        Song current = player.getCurrentSong();
        if (current != null) {
            title.setText(current.getTitle());
            artist.setText(current.getArtist());

            String customCover = viewModel.getSongCover(current.getPath());
            Object model = (customCover != null) ? customCover : current.getAlbumArt();
            Glide.with(this).load(model).placeholder(R.drawable.ic_music).centerCrop().into(albumArt);

            updatePlayPause();
            updateFavoriteIcon(current);
            updatePulseAnimationState(player.isPlaying());
        }
    }

    private void updatePlayPause() {
        btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause_circle : R.drawable.ic_play_circle);
    }

    private void updateFavoriteIcon(Song song) {
        if (song != null) btnFavorite.setImageResource(viewModel.isFavorite(song) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private String formatTime(int millis) {
        return String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }


    private void startPulseAnimation() {
        if (pulseBackground != null && pulseAnimation != null && getContext() != null) {
            pulseBackground.setVisibility(View.VISIBLE);
            pulseBackground.startAnimation(pulseAnimation);

            int themeColor = ThemeHelper.getThemeColor(getContext());
            pulseBackground.getBackground().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void stopPulseAnimation() {
        if (pulseBackground != null) {
            pulseBackground.clearAnimation();
            pulseBackground.setVisibility(View.GONE);
            // Clear the color filter
            pulseBackground.getBackground().setColorFilter(null);
        }
    }

    private void updatePulseAnimationState(boolean isPlaying) {
        if (isPlaying) {
            startPulseAnimation();
        } else {
            stopPulseAnimation();
        }
    }


    @Override
    public void onTrackChanged(Song song, int index) {
        if (!isAdded() || getView() == null) return;
        updateUI();
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        updatePlayPause();
        updatePulseAnimationState(isPlaying);
    }

    @Override public void onError(Exception e) {}
    @Override public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateSeek);
        stopPulseAnimation();
    }
}