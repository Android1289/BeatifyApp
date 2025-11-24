package com.example.beatify;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.IOException;

public class CreateFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView playlistCover;
    private SharedViewModel viewModel;

    private LinearLayout createRoot;
    private MaterialButton btnCreatePlaylist, btnDownloadMp3;
    private TextInputEditText inputYoutubeLink;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        createRoot = v.findViewById(R.id.createRoot);
        btnCreatePlaylist = v.findViewById(R.id.btnCreatePlaylist);
        btnDownloadMp3 = v.findViewById(R.id.btnDownloadMp3);
        inputYoutubeLink = v.findViewById(R.id.inputYoutubeLink);

        applyTheme();

        btnCreatePlaylist.setOnClickListener(view -> showCreatePlaylistDialog());

        btnDownloadMp3.setOnClickListener(view -> {
            String link = inputYoutubeLink.getText().toString().trim();
            if(link.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a valid link", Toast.LENGTH_SHORT).show();
            } else {
                startYoutubeExtraction(link);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        applyTheme();
    }

    private void applyTheme() {
        if (getContext() == null) return;
        ThemeHelper.applyTheme(createRoot, 4, getContext());
        ThemeHelper.applySolidTheme(btnCreatePlaylist, getContext());
    }

    private void startYoutubeExtraction(String youtubeUrl) {
        Toast.makeText(getContext(), "Extracting Audio URL...", Toast.LENGTH_SHORT).show();
        btnDownloadMp3.setEnabled(false); // Prevent double clicks

        new Thread(() -> {
            try {
                YoutubeDLRequest request = new YoutubeDLRequest(youtubeUrl);
                request.addOption("-f", "bestaudio[ext=m4a]");

                VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
                String directUrl = streamInfo.getUrl();
                String title = streamInfo.getTitle();

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getContext() != null) {
                        downloadFile(directUrl, title);
                        btnDownloadMp3.setEnabled(true);
                        inputYoutubeLink.setText(""); // Clear input
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if(getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnDownloadMp3.setEnabled(true);
                    }
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void downloadFile(String url, String fileName) {
        try {
            String safeFileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".m4a";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("Downloading audio from Beatify");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, safeFileName);

            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(getContext(), "Download Started!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Download Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_playlist, null);
        EditText playlistName = dialogView.findViewById(R.id.playlistNameInput);
        playlistCover = dialogView.findViewById(R.id.playlistCover);
        imageUri = null;

        playlistCover.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Cover"), PICK_IMAGE_REQUEST);
        });

        new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setPositiveButton("Create", (d, w) -> {
                    String name = playlistName.getText().toString();
                    if (!name.isEmpty()) {
                        viewModel.addPlaylist(name, (imageUri != null) ? imageUri.toString() : null);
                        Toast.makeText(getContext(), "Playlist Created!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                playlistCover.setImageBitmap(bitmap);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}