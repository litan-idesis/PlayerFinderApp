package com.example.playerfinderapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.playerfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddFriendsActivity extends AppCompatActivity {
    private LinearLayout friendsLayout;
    private List<String> selectedFriends;
    private String groupId;
    private Map<String, String> friendsMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);

        friendsLayout = findViewById(R.id.friends_layout);
        selectedFriends = new ArrayList<>();
        friendsMap = new HashMap<>();
        groupId = getIntent().getStringExtra("groupId");

        fetchFriends();

        Button addFriendsButton = findViewById(R.id.add_friends_button);
        addFriendsButton.setOnClickListener(v -> addSelectedFriends());
    }

    private void fetchFriends() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No friends found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String friendId = document.getId();
                        String friendName = document.getString("name");
                        if (friendName == null) friendName = "Unknown Friend";
                        addFriendCheckbox(friendId, friendName);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addFriendCheckbox(String friendId, String friendName) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(friendName);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedFriends.add(friendId);
            } else {
                selectedFriends.remove(friendId);
            }
        });
        friendsLayout.addView(checkBox);
        friendsMap.put(friendId, friendName);
    }

    private void addSelectedFriends() {
        if (selectedFriends.isEmpty()) {
            Toast.makeText(this, "No friends selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.runTransaction(transaction -> {
            for (String friendId : selectedFriends) {
                transaction.set(
                        db.collection("users").document(currentUserId).collection("friends").document(friendId),
                        new HashMap<>()
                );
            }
            transaction.update(
                    db.collection("groups").document(groupId),
                    "members",
                    FieldValue.arrayUnion(selectedFriends.toArray())
            );
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Friends added successfully.", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error adding friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
