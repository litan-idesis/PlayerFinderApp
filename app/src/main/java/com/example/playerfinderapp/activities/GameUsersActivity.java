package com.example.playerfinderapp.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerfinderapp.R;
import com.example.playerfinderapp.adapters.UserAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameUsersActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView usersRecyclerView;
    private UserAdapter userAdapter;
    private List<Map<String, Object>> userList;
    private String gameName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_users);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        gameName = getIntent().getStringExtra("GAME_NAME");
        if (gameName == null) {
            Toast.makeText(this, "Error: No game selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageButton backButton = findViewById(R.id.back_button);
        TextView titleTextView = findViewById(R.id.title_text);
        usersRecyclerView = findViewById(R.id.users_recyclerview);

        titleTextView.setText("Players of " + gameName);

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);

        backButton.setOnClickListener(v -> finish());

        loadUsersForGame();
    }

    private void loadUsersForGame() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        db.collection("users")
                .whereArrayContains("favoriteGames", gameName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getString("uid");

                        if (userId != null && !currentUserId.equals(userId)) {
                            Map<String, Object> userData = document.getData();
                            userList.add(userData);
                        } else if (userId == null) {
                            // Log a warning if `uid` is missing
                            Toast.makeText(this, "Warning: Skipping user with missing UID", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (userList.isEmpty()) {
                        Toast.makeText(this, "No other users found for " + gameName, Toast.LENGTH_SHORT).show();
                    }

                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
