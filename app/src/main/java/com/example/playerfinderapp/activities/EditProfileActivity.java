package com.example.playerfinderapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerfinderapp.R;
import com.example.playerfinderapp.adapters.GamesSelectionAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private ImageButton backButton;
    private ImageView profilePic;
    private EditText usernameInput, bioInput;
    private RecyclerView gamesRecyclerView;
    private Button saveButton;
    private List<String> selectedGames;
    private GamesSelectionAdapter gamesAdapter;

    private Uri profilePicUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        profilePicUri = imageUri;
                        profilePic.setImageURI(profilePicUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        storageRef = storage.getReference("profile_pictures");

        selectedGames = new ArrayList<>();

        initializeViews();
        loadCurrentData();
        setupListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        profilePic = findViewById(R.id.profile_picture);
        usernameInput = findViewById(R.id.username_input);
        bioInput = findViewById(R.id.bio_input);
        gamesRecyclerView = findViewById(R.id.games_recycler_view);
        saveButton = findViewById(R.id.save_button);

        gamesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        gamesAdapter = new GamesSelectionAdapter(getPopularGames(), selectedGames);
        gamesRecyclerView.setAdapter(gamesAdapter);
    }

    private void loadCurrentData() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        usernameInput.setText(documentSnapshot.getString("username"));
                        bioInput.setText(documentSnapshot.getString("bio"));
                        List<String> games = (List<String>) documentSnapshot.get("favoriteGames");
                        if (games != null) {
                            selectedGames.clear();
                            selectedGames.addAll(games);
                            gamesAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveProfileChanges());
        profilePic.setOnClickListener(v -> openFileChooser());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, getString(R.string.select_profile_picture)));
    }

    private void saveProfileChanges() {
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        String username = usernameInput.getText().toString().trim();
        String bio = bioInput.getText().toString().trim();

        if (username.isEmpty()) {
            usernameInput.setError("Username cannot be empty");
            return;
        }
        if (bio.length() > 150) {
            bioInput.setError("Bio cannot exceed 150 characters");
            return;
        }

        if (profilePicUri != null) {
            uploadProfilePicture(userId, username, bio);
        } else {
            updateFirestore(userId, username, bio, null);
        }
    }

    private void uploadProfilePicture(String userId, String username, String bio) {
        StorageReference fileRef = storageRef.child(userId + ".jpg");
        fileRef.putFile(profilePicUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        updateFirestore(userId, username, bio, uri.toString()))
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error uploading profile picture" + e.getMessage(), Toast.LENGTH_SHORT).show())
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error uploading profile picture" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateFirestore(String userId, String username, String bio, @Nullable String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("bio", bio);
        updates.put("favoriteGames", selectedGames);

        if (imageUrl != null) {
            updates.put("profilePicUrl", imageUrl);
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private List<String> getPopularGames() {
        List<String> games = new ArrayList<>();
        games.add("ROBLOX");
        games.add("Minecraft");
        games.add("Fortnite");
        games.add("Counter-Strike 2 & GO");
        games.add("Call of Duty");
        games.add("League of Legends");
        games.add("Valorant");
        return games;
    }
}
