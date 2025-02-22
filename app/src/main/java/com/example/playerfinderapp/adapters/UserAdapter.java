package com.example.playerfinderapp.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<Map<String, Object>> userList;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public UserAdapter(List<Map<String, Object>> userList) {
        this.userList = userList;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_list_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        Map<String, Object> user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        ImageView addFriendButton;

        public UserViewHolder(View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.username_text_view);
            addFriendButton = itemView.findViewById(R.id.add_friend_button);

            addFriendButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    addFriend(userList.get(position));
                }
            });
        }

        public void bind(Map<String, Object> user) {
            usernameTextView.setText((String) user.get("username"));
        }

        private void addFriend(Map<String, Object> friendUser) {
            String currentUserId = auth.getCurrentUser().getUid();
            String friendUserId = (String) friendUser.get("uid"); // Make sure your user data contains "uid"

            if (currentUserId.equals(friendUserId)) {
                Toast.makeText(itemView.getContext(), "Cannot add yourself as friend", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> friendData = new HashMap<>();
            friendData.put("uid", friendUserId);
            friendData.put("username", friendUser.get("username"));

            db.collection("users").document(currentUserId)
                    .collection("friends")
                    .document(friendUserId)
                    .set(friendData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), "Friend added successfully", Toast.LENGTH_SHORT).show();

                        addFriendButton.setImageResource(R.drawable.ic_check);
                        addFriendButton.setEnabled(false);

                        db.collection("users").document(currentUserId)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    Map<String, Object> currentUserData = new HashMap<>();
                                    currentUserData.put("uid", currentUserId);
                                    currentUserData.put("username", documentSnapshot.getString("username"));

                                    db.collection("users").document(friendUserId)
                                            .collection("friends")
                                            .document(currentUserId)
                                            .set(currentUserData);
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(itemView.getContext(),
                                    "Failed to add friend: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }
}