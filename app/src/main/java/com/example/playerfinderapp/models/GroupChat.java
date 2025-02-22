package com.example.playerfinderapp.models;

import java.util.List;

public class GroupChat {
    private String id;
    private String name;
    private String lastMessage;
    private String imageUrl;
    private List<String> memberIds;
    private String createdBy;

    public GroupChat(String id, String name, String lastMessage, String imageUrl, List<String> memberIds, String createdBy) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.imageUrl = imageUrl;
        this.memberIds = memberIds;
        this.createdBy = createdBy;
    }

    public GroupChat(String name, String imageUrl, List<String> memberIds) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.memberIds = memberIds;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
