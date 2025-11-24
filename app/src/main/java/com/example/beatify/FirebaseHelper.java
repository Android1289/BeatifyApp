package com.example.beatify;

import android.net.Uri;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    public FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    public void signUp(String name, String email, String password, FirebaseAuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser fUser = auth.getCurrentUser();
                if (fUser != null) {
                    User newUser = new User(fUser.getUid(), name, email);
                    db.collection("users").document(fUser.getUid()).set(newUser)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(fUser))
                            .addOnFailureListener(callback::onFailure);
                }
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public void login(String email, String password, FirebaseAuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onSuccess(auth.getCurrentUser());
                    else callback.onFailure(task.getException());
                });
    }

    public void signOut() { auth.signOut(); }

    public void getUserProfile(String uid, UserCallback callback) {
        db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) callback.onCallback(snapshot.toObject(User.class));
            else callback.onCallback(null);
        });
    }

    public void searchUsers(String query, UserListCallback callback) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        users.add(doc.toObject(User.class));
                    }
                    callback.onCallback(users);
                });
    }

    public void uploadProfileImage(Uri imageUri, final ImageUploadCallback callback) {
        if (getCurrentUser() == null) return;
        String path = "images/" + getCurrentUser().getUid() + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    db.collection("users").document(getCurrentUser().getUid())
                            .update("profileImageUrl", uri.toString());
                    callback.onSuccess(uri.toString());
                }).addOnFailureListener(callback::onFailure)
        ).addOnFailureListener(callback::onFailure);
    }

    public void savePlaylist(Playlist playlist) {
        if(getCurrentUser() == null) return;
        playlist.setOwnerId(getCurrentUser().getUid());
        db.collection("users").document(getCurrentUser().getUid())
                .collection("playlists").document(playlist.getId()).set(playlist);
    }

    public void deletePlaylist(Playlist playlist) {
        if(getCurrentUser() == null) return;
        db.collection("users").document(getCurrentUser().getUid())
                .collection("playlists").document(playlist.getId()).delete();
    }

    public void getUserPlaylists(String uid, PlaylistListCallback callback) {
        if(uid == null) { callback.onCallback(new ArrayList<>()); return; }

        db.collection("users").document(uid)
                .collection("playlists").get().addOnSuccessListener(snapshots -> {
                    ArrayList<Playlist> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        list.add(doc.toObject(Playlist.class));
                    }
                    callback.onCallback(list);
                });
    }

    public void isFollowing(String currentUid, String targetUid, IsFollowingCallback callback) {
        if (currentUid == null || targetUid == null) { callback.onCallback(false); return; }
        db.collection("users").document(currentUid).collection("following").document(targetUid).get()
                .addOnSuccessListener(snapshot -> callback.onCallback(snapshot.exists()))
                .addOnFailureListener(e -> callback.onCallback(false));
    }

    public void followUser(String currentUid, String targetUid, Runnable onSuccess) {
        if (currentUid == null || targetUid == null) return;

        db.collection("users").document(currentUid).collection("following").document(targetUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Map<String, Object> data = new HashMap<>(); data.put("uid", targetUid);
                        db.collection("users").document(currentUid).collection("following").document(targetUid).set(data);

                        Map<String, Object> fData = new HashMap<>(); fData.put("uid", currentUid);
                        db.collection("users").document(targetUid).collection("followers").document(currentUid).set(fData);

                        db.collection("users").document(currentUid).update("followingCount", com.google.firebase.firestore.FieldValue.increment(1));
                        db.collection("users").document(targetUid).update("followersCount", com.google.firebase.firestore.FieldValue.increment(1))
                                .addOnSuccessListener(aVoid -> { if(onSuccess != null) onSuccess.run(); });
                    } else {
                        if(onSuccess != null) onSuccess.run();
                    }
                });
    }

    public void unfollowUser(String currentUid, String targetUid, Runnable onSuccess) {
        if (currentUid == null || targetUid == null) return;

        db.collection("users").document(currentUid).collection("following").document(targetUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        db.collection("users").document(currentUid).collection("following").document(targetUid).delete();
                        db.collection("users").document(targetUid).collection("followers").document(currentUid).delete();

                        db.collection("users").document(currentUid).update("followingCount", com.google.firebase.firestore.FieldValue.increment(-1));
                        db.collection("users").document(targetUid).update("followersCount", com.google.firebase.firestore.FieldValue.increment(-1))
                                .addOnSuccessListener(aVoid -> { if(onSuccess != null) onSuccess.run(); });
                    }
                });
    }

    public interface FirebaseAuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
    public interface UserCallback { void onCallback(User user); }
    public interface ImageUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }
    public interface UserListCallback { void onCallback(List<User> users); }
    public interface PlaylistListCallback { void onCallback(ArrayList<Playlist> playlists); }
    public interface IsFollowingCallback { void onCallback(boolean isFollowing); }
}