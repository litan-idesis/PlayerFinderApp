package com.example.playerfinderapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.playerfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText searchEditText;
    private ImageButton searchButton, backButton, clearInputButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        searchEditText = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);
        backButton = findViewById(R.id.back_button);
        clearInputButton = findViewById(R.id.clear_input_button);

        searchButton.setOnClickListener(v -> {
            String searchText = searchEditText.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchUsers(searchText);
            } else {
                Toast.makeText(SearchActivity.this, "Please enter a username to search", Toast.LENGTH_SHORT).show();
            }
        });

        clearInputButton.setOnClickListener(v -> searchEditText.setText(""));
        backButton.setOnClickListener(v -> finish());
    }

    private void searchUsers(String searchText) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to search", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        db.collection("users")
                .whereGreaterThanOrEqualTo("username", searchText)
                .whereLessThanOrEqualTo("username", searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> searchResults = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getString("uid");
                        if (userId != null && !userId.equals(currentUserId)) {
                            Map<String, Object> userData = document.getData();
                            if (userData != null) {
                                searchResults.add(userData);
                            }
                        }
                    }

                    if (searchResults.isEmpty()) {
                        Toast.makeText(SearchActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(SearchActivity.this, SearchResultsActivity.class);
                        intent.putExtra("searchResults", new ArrayList<>(searchResults));
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchActivity.this, "Error searching users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("SearchActivity", "Search error", e);
                });
    }
}
