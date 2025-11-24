package com.example.beatify;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class UserDetailsFragment extends Fragment {
    private SharedViewModel viewModel;
    private TextView name, followers, following, pinnedTitle;
    private MaterialButton btnAction;
    private RecyclerView recyclerPinned;
    private ImageView profileImage;
    private FirebaseHelper firebaseHelper;
    private String targetUserId = null;
    private boolean isLoginMode = true;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String uid = null;
                    if (viewModel.isUserLoggedIn() && firebaseHelper.getCurrentUser() != null) {
                        uid = firebaseHelper.getCurrentUser().getUid();
                    } else {
                        uid = "local_guest";
                    }
                    viewModel.saveLocalProfileImage(uid, uri.toString());
                    Glide.with(UserDetailsFragment.this).load(uri).into(profileImage);
                    Toast.makeText(getContext(), "Profile Picture Updated Locally", Toast.LENGTH_SHORT).show();
                }
            });

    public static UserDetailsFragment newInstance(String uid) {
        UserDetailsFragment f = new UserDetailsFragment();
        Bundle args = new Bundle(); args.putString("uid", uid); f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_details, container, false);
        if (getArguments() != null) targetUserId = getArguments().getString("uid");

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        firebaseHelper = new FirebaseHelper();

        name = v.findViewById(R.id.profileName);
        followers = v.findViewById(R.id.profileFollowers);
        following = v.findViewById(R.id.profileFollowing);
        btnAction = v.findViewById(R.id.btnAction);
        recyclerPinned = v.findViewById(R.id.recyclerPinned);
        profileImage = v.findViewById(R.id.profileImage);
        pinnedTitle = v.findViewById(R.id.pinnedTitle);

        recyclerPinned.setLayoutManager(new LinearLayoutManager(getContext()));
        setupUI();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(getView(), 3, getContext());
        ThemeHelper.applySolidTheme(btnAction, getContext());
    }

    private void setupUI() {
        if (targetUserId != null && !targetUserId.equals(firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : "")) {
            firebaseHelper.getUserProfile(targetUserId, user -> {
                if (user != null) {
                    name.setText(user.getName());
                    followers.setText(user.getFollowersCount() + " Followers");
                    following.setText(user.getFollowingCount() + " Following");

                    checkFollowStatus(user);

                    String pfpUrl = user.getProfileImageUrl();
                    if (pfpUrl == null || pfpUrl.isEmpty()) {
                        pfpUrl = viewModel.getLocalProfileImage(targetUserId);
                    }

                    if (pfpUrl != null && !pfpUrl.isEmpty()) {
                        Glide.with(this).load(pfpUrl).placeholder(R.drawable.ic_user_placeholder).into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.ic_user_placeholder);
                    }

                    recyclerPinned.setVisibility(View.VISIBLE);
                    firebaseHelper.getUserPlaylists(targetUserId, playlists -> {
                        ArrayList<Playlist> pinned = new ArrayList<>();
                        for (Playlist p : playlists) if (p.isPinned()) pinned.add(p);

                        PlaylistAdapter adapter = new PlaylistAdapter(getContext(), pinned, new PlaylistAdapter.PlaylistActionListener() {
                            @Override public void onPlaylistClick(Playlist p) {
                                viewModel.selectPlaylist(p);
                                ((MainActivity)requireActivity()).showFragment(new PlaylistDetailFragment());
                            }
                            @Override public void onPinClick(Playlist p) {}
                            @Override public void onDeleteClick(Playlist p) {}
                            @Override public void onEditClick(Playlist p) {}
                        });
                        recyclerPinned.setAdapter(adapter);
                    });
                }
            });
            return;
        }

        if (viewModel.isUserLoggedIn()) {
            viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    name.setText(user.getName());
                    followers.setText(user.getFollowersCount() + " Followers");
                    following.setText(user.getFollowingCount() + " Following");

                    String pfp = user.getProfileImageUrl();
                    if(pfp == null || pfp.isEmpty()) pfp = viewModel.getLocalProfileImage(user.getUid());

                    if(pfp != null && !pfp.isEmpty()) {
                        Glide.with(this).load(pfp).placeholder(R.drawable.ic_user_placeholder).into(profileImage);
                    }
                }
            });
            btnAction.setText("Sign Out");
            btnAction.setOnClickListener(v -> { firebaseHelper.signOut(); viewModel.setLoggedIn(false); setupUI(); });
            profileImage.setOnClickListener(v -> pickImage.launch("image/*"));
            showPinnedPlaylists();
        } else {
            name.setText("Guest");
            followers.setText("0 Followers");
            following.setText("0 Following");

            String localPfp = viewModel.getLocalProfileImage("local_guest");
            if(localPfp != null) Glide.with(this).load(localPfp).into(profileImage);

            btnAction.setText("Sign In");
            btnAction.setOnClickListener(v -> showLoginDialog());
            profileImage.setOnClickListener(v -> pickImage.launch("image/*"));
            showPinnedPlaylists();
        }
    }

    private void checkFollowStatus(User user) {
        String currentUid = viewModel.isUserLoggedIn() && firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
        if (currentUid == null) {
            btnAction.setText("Follow");
            btnAction.setOnClickListener(v -> Toast.makeText(getContext(), "Login required", Toast.LENGTH_SHORT).show());
            return;
        }

        firebaseHelper.isFollowing(currentUid, user.getUid(), isFollowing -> {
            if (!isAdded()) return;

            if (isFollowing) {
                btnAction.setText("Following");
                btnAction.setOnClickListener(v -> {
                    firebaseHelper.unfollowUser(currentUid, user.getUid(), () -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(), "Unfollowed", Toast.LENGTH_SHORT).show();
                        user.setFollowersCount(user.getFollowersCount() - 1);
                        followers.setText(user.getFollowersCount() + " Followers");
                        checkFollowStatus(user);
                    });
                });
            } else {
                btnAction.setText("Follow");
                btnAction.setOnClickListener(v -> {
                    firebaseHelper.followUser(currentUid, user.getUid(), () -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(), "Followed!", Toast.LENGTH_SHORT).show();
                        user.setFollowersCount(user.getFollowersCount() + 1);
                        followers.setText(user.getFollowersCount() + " Followers");
                        checkFollowStatus(user);
                    });
                });
            }
        });
    }

    private void showPinnedPlaylists() {
        viewModel.getPlaylists().observe(getViewLifecycleOwner(), list -> {
            ArrayList<Playlist> pinned = new ArrayList<>();
            if(list != null) {
                for (Playlist p : list) if (p.isPinned()) pinned.add(p);
            }
            PlaylistAdapter adapter = new PlaylistAdapter(getContext(), pinned, new PlaylistAdapter.PlaylistActionListener() {
                @Override public void onPlaylistClick(Playlist p) { viewModel.selectPlaylist(p); ((MainActivity)requireActivity()).showFragment(new PlaylistDetailFragment()); }
                @Override public void onPinClick(Playlist p) { viewModel.togglePin(p); }
                @Override public void onDeleteClick(Playlist p) { viewModel.deletePlaylist(p); }
                @Override public void onEditClick(Playlist p) {}
            });
            recyclerPinned.setAdapter(adapter);
        });
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_login, null);

        TextInputLayout layoutName = view.findViewById(R.id.layoutName);
        TextInputEditText inputName = view.findViewById(R.id.inputName);
        TextInputEditText inputEmail = view.findViewById(R.id.inputEmail);
        TextInputEditText inputPassword = view.findViewById(R.id.inputPassword);
        MaterialButton btnPerform = view.findViewById(R.id.btnPerformAuth);
        TextView txtToggle = view.findViewById(R.id.txtToggleMode);
        TextView title = view.findViewById(R.id.loginTitle);

        builder.setView(view);
        AlertDialog dialog = builder.create();
        isLoginMode = true;
        updateDialogState(true, layoutName, btnPerform, txtToggle, title);

        txtToggle.setOnClickListener(v -> { isLoginMode = !isLoginMode; updateDialogState(isLoginMode, layoutName, btnPerform, txtToggle, title); });

        btnPerform.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass = inputPassword.getText().toString().trim();
            String uName = inputName.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) return;

            if (isLoginMode) {
                firebaseHelper.login(email, pass, new FirebaseHelper.FirebaseAuthCallback() {
                    @Override public void onSuccess(com.google.firebase.auth.FirebaseUser user) { viewModel.setLoggedIn(true); setupUI(); dialog.dismiss(); }
                    @Override public void onFailure(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
            } else {
                firebaseHelper.signUp(uName, email, pass, new FirebaseHelper.FirebaseAuthCallback() {
                    @Override public void onSuccess(com.google.firebase.auth.FirebaseUser user) { viewModel.setLoggedIn(true); setupUI(); dialog.dismiss(); }
                    @Override public void onFailure(Exception e) { Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
            }
        });
        dialog.show();
    }

    private void updateDialogState(boolean isLogin, View layoutName, MaterialButton btn, TextView toggle, TextView title) {
        title.setText(isLogin ? "Sign In" : "Sign Up");
        layoutName.setVisibility(isLogin ? View.GONE : View.VISIBLE);
        btn.setText(isLogin ? "Login" : "Sign Up");
        toggle.setText(isLogin ? "New user? Sign Up" : "Already have an account? Login");
    }
}