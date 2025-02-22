package com.example.playerfinderapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerfinderapp.R;
import com.example.playerfinderapp.adapters.MessageAdapter;
import com.example.playerfinderapp.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private TextView usernameTextView;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration messageListener;
    private String chatId;
    private String friendId;
    private String friendName;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra("chatId");
        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        recyclerViewMessages = findViewById(R.id.messages_recycler_view);
        editTextMessage = findViewById(R.id.message_input);
        buttonSend = findViewById(R.id.send_button);
        usernameTextView = findViewById(R.id.username);
        ImageButton returnButton = findViewById(R.id.return_button);

        usernameTextView.setText(friendName);
        usernameTextView.setVisibility(View.VISIBLE);
        returnButton.setOnClickListener(v -> finish());

        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messages, currentUserId);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        loadMessages();

        buttonSend.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString().trim();
            if (messageText.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(messageText);
        });
    }

    private void loadMessages() {
        if (chatId == null) return;

        Query query = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        messageListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error loading messages:", error);
                return;
            }

            messages.clear();
            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot doc : value) {
                    Message message = doc.toObject(Message.class);
                    messages.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                recyclerViewMessages.scrollToPosition(messages.size() - 1);
            } else {
                Log.w(TAG, "No messages found for chat.");
            }
        });
    }

    private void sendMessage(String messageText) {
        if (chatId == null) return;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", messageText);
        messageData.put("senderId", currentUserId);
        messageData.put("timestamp", FieldValue.serverTimestamp());

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    editTextMessage.setText("");
                    updateLastMessage(messageText);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error sending message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void updateLastMessage(String messageText) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", messageText);
        updates.put("lastMessageTime", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(currentUserId)
                .collection("chats")
                .document(chatId)
                .update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating last message:", e));

        firestore.collection("users")
                .document(friendId)
                .collection("chats")
                .document(chatId)
                .update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating last message for friend:", e));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}