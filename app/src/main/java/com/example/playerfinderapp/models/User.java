package com.example.playerfinderapp.models;

public class User {
    private String id;
    private String username;
    private String profilePictureUrl;

        public User() {
    }

    public User(String id, String username, String profilePictureUrl) {
        this.id = id;
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
