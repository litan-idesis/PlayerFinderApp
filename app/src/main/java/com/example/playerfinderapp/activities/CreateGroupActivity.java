package com.example.playerfinderapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerfinderapp.R;
import com.example.playerfinderapp.adapters.MemberAdapter;
import com.example.playerfinderapp.models.GroupChat;
import com.example.playerfinderapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageButton returnButton;
    private EditText groupNameEditText;
    private ImageButton groupImageButton;
    private SearchView memberSearchView;
    private RecyclerView membersRecyclerView;
    private Button createGroupButton;
    private MemberAdapter memberAdapter;
    private List<User> allMembers; // All friends
    private List<User> selectedMembers;
    private Uri groupImageUri;
    private String groupImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        returnButton = findViewById(R.id.return_button);
        groupNameEditText = findViewById(R.id.group_name_edit_text);
        groupImageButton = findViewById(R.id.group_image_button);
        memberSearchView = findViewById(R.id.member_search_view);
        membersRecyclerView = findViewById(R.id.members_recycler_view);
        createGroupButton = findViewById(R.id.create_group_button);

        allMembers = new ArrayList<>();
        selectedMembers = new ArrayList<>();


        loadAllMembers();

        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new MemberAdapter(allMembers, selectedMembers);
        membersRecyclerView.setAdapter(memberAdapter);

        returnButton.setOnClickListener(v -> finish());
        groupImageButton.setOnClickListener(v -> openFileChooser());

        memberSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                memberAdapter.filter(newText);
                return true;
            }
        });

        createGroupButton.setOnClickListener(v -> createGroup());
    }

    private void loadAllMembers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(currentUserId).collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        allMembers.add(user);
                    }
                    memberAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Group Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            groupImageUri = data.getData();
            groupImageButton.setImageURI(groupImageUri);
        }
    }

    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();

        if (groupName.isEmpty()) {
            Toast.makeText(this, "Please enter a group name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "Please select at least one member.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (groupImageUri != null) {
            uploadGroupImage(groupName);
        } else {
            saveGroupToFirestore(groupName, null);
        }
    }

    private void uploadGroupImage(String groupName) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("group_images/" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(groupImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveGroupToFirestore(groupName, uri.toString())))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveGroupToFirestore(String groupName, @Nullable String imageUrl) {
        List<String> memberIds = new ArrayList<>();
        for (User member : selectedMembers) {
            memberIds.add(member.getId());
        }

        GroupChat group = new GroupChat(groupName, imageUrl, memberIds);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("groups")
                .add(group)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
