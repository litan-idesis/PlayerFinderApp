package com.example.playerfinderapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerfinderapp.R;
import com.example.playerfinderapp.adapters.FriendsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateNewChatActivity extends AppCompatActivity {
    private static final String TAG = "CreateNewChatActivity";

    private List<Map<String, Object>> friendsList;
    private FriendsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private RecyclerView friendsRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_chat);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        String friendId = getIntent().getStringExtra("friendId");
        String friendName = getIntent().getStringExtra("friendName");

        if (friendId != null && friendName != null) {
            createChat(friendId, friendName);
            return;
        }

        friendsList = new ArrayList<>();
        friendsRecyclerView = findViewById(R.id.friends_recycler_view);
        EditText searchEditText = findViewById(R.id.search_username);
        ImageButton returnButton = findViewById(R.id.return_button);

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendsAdapter(friendsList, false) {
            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                holder.itemView.setOnClickListener(v -> {
                    Map<String, Object> friend = friendsList.get(position);
                    createChat((String) friend.get("uid"), (String) friend.get("username"));
                });
            }
        };
        friendsRecyclerView.setAdapter(adapter);

        fetchFriends();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        returnButton.setOnClickListener(v -> finish());
    }

    private void fetchFriends() {
        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    friendsList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getString("uid");
                        String username = doc.getString("username");
                        if (uid != null && username != null) {
                            Map<String, Object> friend = new HashMap<>();
                            friend.put("uid", uid);
                            friend.put("username", username);
                            friendsList.add(friend);
                        } else {
                            Log.w(TAG, "Skipping friend with missing data: " + doc.getId());
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterFriends(String searchText) {
        if (searchText.isEmpty()) {
            fetchFriends();
            return;
        }

        List<Map<String, Object>> filteredList = new ArrayList<>();
        for (Map<String, Object> friend : friendsList) {
            String username = (String) friend.get("username");
            if (username != null && username.toLowerCase().contains(searchText.toLowerCase())) {
                filteredList.add(friend);
            }
        }

        friendsList.clear();
        friendsList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    public void createChat(String friendId, String friendName) {
        String chatId = generateChatId(currentUserId, friendId);

        db.collection("users")
                .document(currentUserId)
                .collection("chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        navigateToChat(chatId, friendId, friendName);
                    } else {
                        createNewChat(chatId, friendId, friendName);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewChat(String chatId, String friendId, String friendName) {
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(currentUserDoc -> {
                    String currentUsername = currentUserDoc.getString("username");
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("participants", List.of(currentUserId, friendId));
                    chatData.put("createdAt", FieldValue.serverTimestamp());
                    chatData.put("lastMessage", "");
                    chatData.put("lastMessageTime", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId)
                            .set(chatData)
                            .addOnSuccessListener(aVoid -> {
                                createUserChatReferences(chatId, friendId, friendName, currentUsername);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error creating chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void createUserChatReferences(String chatId, String friendId, String friendName, String currentUsername) {
        Map<String, Object> userChat = Map.of(
                "chatId", chatId,
                "friendId", friendId,
                "friendName", friendName,
                "lastMessage", "",
                "lastMessageTime", FieldValue.serverTimestamp()
        );

        db.collection("users").document(currentUserId).collection("chats").document(chatId)
                .set(userChat)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> friendChat = Map.of(
                            "chatId", chatId,
                            "friendId", currentUserId,
                            "friendName", currentUsername,
                            "lastMessage", "",
                            "lastMessageTime", FieldValue.serverTimestamp()
                    );
                    db.collection("users").document(friendId).collection("chats").document(chatId)
                            .set(friendChat)
                            .addOnSuccessListener(aVoid1 -> navigateToChat(chatId, friendId, friendName));
                });
    }

    private String generateChatId(String userId1, String userId2) {
        List<String> ids = new ArrayList<>();
        ids.add(userId1);
        ids.add(userId2);

        Collections.sort(ids);
        return String.join("_", ids);
    }

    private void navigateToChat(String chatId, String friendId, String friendName) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("friendId", friendId);
        intent.putExtra("friendName", friendName);
        startActivity(intent);
        finish();
    }
}
