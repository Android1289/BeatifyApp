package com.example.beatify;

public class User {
    private String uid;
    private String name;
    private String email;
    private int followersCount;
    private int followingCount;
    private String profileImageUrl;

    public User() {}

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.followersCount = 0;
        this.followingCount = 0;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public int getFollowersCount() { return followersCount; }

    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}