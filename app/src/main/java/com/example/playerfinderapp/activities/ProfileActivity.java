package com.example.playerfinderapp.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerfinderapp.R;
import com.example.playerfinderapp.adapters.FriendsAdapter;
import com.example.playerfinderapp.adapters.GamesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;
    private boolean isOwnProfile;

    private ImageButton backButton, settingsButton, addFriendButton;
    private ImageView profilePicture;
    private TextView usernameText, bioText;
    private RecyclerView favoriteGamesList, friendsList;
    private GamesAdapter gamesAdapter;
    private FriendsAdapter friendsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            userId = auth.getCurrentUser().getUid();
            isOwnProfile = true;
        } else {
            isOwnProfile = userId.equals(auth.getCurrentUser().getUid());
        }

        initializeViews();
        setupButtons();
        loadProfileData();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        settingsButton = findViewById(R.id.settings_button);
        addFriendButton = findViewById(R.id.add_friend_button);
        profilePicture = findViewById(R.id.profile_picture);
        usernameText = findViewById(R.id.username_text);
        bioText = findViewById(R.id.bio_text);
        favoriteGamesList = findViewById(R.id.favorite_games_list);
        friendsList = findViewById(R.id.friends_list);

        favoriteGamesList.setLayoutManager(new LinearLayoutManager(this));
        friendsList.setLayoutManager(new LinearLayoutManager(this));

        settingsButton.setVisibility(isOwnProfile ? View.VISIBLE : View.GONE);
        addFriendButton.setVisibility(isOwnProfile ? View.GONE : View.VISIBLE);
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> finish());

        settingsButton.setOnClickListener(v -> {
            if (isOwnProfile) {
                showSettingsDialog();
            }
        });

        addFriendButton.setOnClickListener(v -> {
            if (!isOwnProfile) {
                addFriend();
            }
        });
    }

    private void loadProfileData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");
                        List<String> games = (List<String>) documentSnapshot.get("favoriteGames");

                        usernameText.setText(username != null ? username : "Unknown User");
                        bioText.setText(bio != null ? bio : "No bio available.");

                        if (games != null) {
                            gamesAdapter = new GamesAdapter(games);
                            favoriteGamesList.setAdapter(gamesAdapter);
                        }

                        loadFriends();
                    } else {
                        Toast.makeText(this, "User profile does not exist.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadFriends() {
        db.collection("users").document(userId)
                .collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> friends = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        friends.add(doc.getData());
                    }
                    friendsAdapter = new FriendsAdapter(friends, isOwnProfile);
                    friendsList.setAdapter(friendsAdapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading friends: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addFriend() {
        String currentUserId = auth.getCurrentUser().getUid();

        if (currentUserId.equals(userId)) {
            Toast.makeText(this, "Cannot add yourself as a friend.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.runTransaction(transaction -> {
            Map<String, Object> currentUserData = Map.of("uid", currentUserId);
            Map<String, Object> friendData = Map.of("uid", userId);

            transaction.set(db.collection("users").document(currentUserId).collection("friends").document(userId), friendData);
            transaction.set(db.collection("users").document(userId).collection("friends").document(currentUserId), currentUserData);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Friend added successfully!", Toast.LENGTH_SHORT).show();
            addFriendButton.setEnabled(false);
        }).addOnFailureListener(e -> Toast.makeText(this, "Error adding friend: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showSettingsDialog() {
        String[] options = {"Edit Profile", "Log Out", "Delete Account"};
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, EditProfileActivity.class));
                    } else if (which == 1) {
                        auth.signOut();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else if (which == 2) {
                        deleteAccount();
                    }
                })
                .show();
    }

    private void deleteAccount() {
        FirebaseAuth.getInstance().getCurrentUser().delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
