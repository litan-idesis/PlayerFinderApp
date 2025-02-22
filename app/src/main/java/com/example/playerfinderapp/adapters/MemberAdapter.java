package com.example.playerfinderapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.playerfinderapp.R;
import com.example.playerfinderapp.models.User;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
    private List<User> allMembers;
    private List<User> filteredMembers;
    private List<User> selectedMembers;

    public MemberAdapter(List<User> allMembers, List<User> selectedMembers) {
        this.allMembers = allMembers;
        this.filteredMembers = new ArrayList<>(allMembers);
        this.selectedMembers = selectedMembers;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = filteredMembers.get(position);

        Glide.with(holder.itemView.getContext())
                .load(user.getProfilePictureUrl())
                .placeholder(R.drawable.ic_profile_picture_default)
                .into(holder.profileImageView);

        holder.usernameTextView.setText(user.getUsername());

        holder.checkbox.setChecked(selectedMembers.contains(user));

        holder.checkbox.setOnClickListener(v -> {
            if (holder.checkbox.isChecked()) {
                selectedMembers.add(user);
            } else {
                selectedMembers.remove(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredMembers.size();
    }

    public void filter(String query) {
        filteredMembers.clear();
        if (query.isEmpty()) {
            filteredMembers.addAll(allMembers);
        } else {
            for (User user : allMembers) {
                if (user.getUsername().toLowerCase().contains(query.toLowerCase())) {
                    filteredMembers.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView;
        CheckBox checkbox;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profile_image);
            usernameTextView = itemView.findViewById(R.id.username_text_view);
            checkbox = itemView.findViewById(R.id.checkbox);
        }
    }
}
