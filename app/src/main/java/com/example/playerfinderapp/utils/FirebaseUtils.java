package com.example.playerfinderapp.utils;

import android.net.Uri;
import android.util.Log;

import com.example.playerfinderapp.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FirebaseUtils {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();

    public interface UsersListCallback {
        void onUsersListLoaded(List<User> users);
    }

    public interface FriendsListCallback {
        void onFriendsListLoaded(List<User> friends);
    }

    public interface ImageUploadCallback {
        void onImageUploaded(String imageUrl);
    }

    public static void getUsersList(UsersListCallback callback) {
        List<User> users = new ArrayList<>();

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null && !result.isEmpty()) {
                            for (QueryDocumentSnapshot document : result) {
                                String id = document.getId();
                                String username = document.getString("username");
                                String profilePictureUrl = document.getString("profilePictureUrl");

                                if (username == null) username = "Unknown User";
                                users.add(new User(id, username, profilePictureUrl));
                            }
                        } else {
                            Log.w("FirebaseUtils", "No users found in Firestore.");
                        }
                        callback.onUsersListLoaded(users);
                    } else {
                        Log.e("FirebaseUtils", "Error getting users list: ", task.getException());
                        callback.onUsersListLoaded(new ArrayList<>());
                    }
                });
    }

    public static void getFriendsList(String userId, FriendsListCallback callback) {
        List<User> friends = new ArrayList<>();

        db.collection("users")
                .document(userId)
                .collection("friends")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null && !result.isEmpty()) {
                            for (QueryDocumentSnapshot document : result) {
                                String id = document.getId();
                                String username = document.getString("username");
                                String profilePictureUrl = document.getString("profilePictureUrl");

                                if (username == null) username = "Unknown Friend";
                                friends.add(new User(id, username, profilePictureUrl));
                            }
                        } else {
                            Log.w("FirebaseUtils", "No friends found for user: " + userId);
                        }
                        callback.onFriendsListLoaded(friends);
                    } else {
                        Log.e("FirebaseUtils", "Error getting friends list: ", task.getException());
                        callback.onFriendsListLoaded(new ArrayList<>());
                    }
                });
    }

    public static void uploadImage(Uri imageUri, ImageUploadCallback callback) {
        if (imageUri == null || imageUri.getLastPathSegment() == null) {
            Log.e("FirebaseUtils", "Invalid image URI.");
            callback.onImageUploaded(null);
            return;
        }

        String uniqueFilename = UUID.randomUUID().toString() + "_" + imageUri.getLastPathSegment();
        StorageReference storageRef = storage.getReference().child("group_images/" + uniqueFilename);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String imageUrl = task.getResult().toString();
                        callback.onImageUploaded(imageUrl);
                    } else {
                        Log.e("FirebaseUtils", "Error getting image URL: ", task.getException());
                        callback.onImageUploaded(null);
                    }
                }))
                .addOnFailureListener(e -> {
                    Log.e("FirebaseUtils", "Image upload failed: ", e);
                    callback.onImageUploaded(null);
                });
    }
}
