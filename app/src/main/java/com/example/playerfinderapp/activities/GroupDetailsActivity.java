package com.example.playerfinderapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.playerfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GroupDetailsActivity extends AppCompatActivity {

    private ImageView groupProfilePicture;
    private EditText groupNameEditText, groupBioEditText;
    private Button leaveGroupButton, addFriendsButton;
    private boolean isAdmin;
    private Uri groupImageUri;
    private String groupId;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_details);

        groupProfilePicture = findViewById(R.id.group_profile_picture);
        groupNameEditText = findViewById(R.id.group_name);
        groupBioEditText = findViewById(R.id.group_bio);
        leaveGroupButton = findViewById(R.id.leave_group_button);
        addFriendsButton = findViewById(R.id.add_friends_button);

        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            Toast.makeText(this, "Error: Group ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> admins = (List<String>) documentSnapshot.get("admins");
                        isAdmin = admins != null && admins.contains(currentUserId);
                        updateUIForAdmin();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error retrieving group data.", Toast.LENGTH_SHORT).show();
                });

        leaveGroupButton.setOnClickListener(v -> leaveGroup());
        addFriendsButton.setOnClickListener(v -> addFriends());
    }

    private void updateUIForAdmin() {
        if (!isAdmin) {
            addFriendsButton.setVisibility(View.GONE);
        }
    }

    private void leaveGroup() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> members = (List<String>) documentSnapshot.get("members");
                        List<String> admins = (List<String>) documentSnapshot.get("admins");

                        if (admins.contains(currentUserId)) {
                            handleAdminLeaving(db, currentUserId, members, admins);
                        } else {
                            handleMemberLeaving(db, currentUserId, members);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error retrieving group data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleAdminLeaving(FirebaseFirestore db, String currentUserId, List<String> members, List<String> admins) {
        if (members.size() > 1) {
            members.remove(currentUserId);
            admins.remove(currentUserId);
            String newAdminId = members.get(new Random().nextInt(members.size()));
            admins.add(newAdminId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("members", members);
            updates.put("admins", admins);

            db.collection("groups").document(groupId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "You left the group. A new admin has been selected.", Toast.LENGTH_SHORT).show();
                        finishActivity();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating group data.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("groups").document(groupId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Group deleted as no members are left.", Toast.LENGTH_SHORT).show();
                        finishActivity();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error deleting the group.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void handleMemberLeaving(FirebaseFirestore db, String currentUserId, List<String> members) {
        members.remove(currentUserId);
        db.collection("groups").document(groupId).update("members", members)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "You have left the group.", Toast.LENGTH_SHORT).show();
                    finishActivity();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating group data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void finishActivity() {
        startActivity(new Intent(this, ChatListActivity.class));
        finish();
    }

    private void addFriends() {
        Intent intent = new Intent(this, AddFriendsActivity.class);
        intent.putExtra("groupId", groupId);
        startActivity(intent);
    }
}
