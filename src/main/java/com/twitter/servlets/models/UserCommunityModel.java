package com.twitter.servlets.models;

public class UserCommunityModel {
    private int CommunityId;
    private String position = "";

    public int getCommunityId() {
        return CommunityId;
    }

    public void setCommunityId(int communityId) {
        CommunityId = communityId;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPosition() {
        return position;
    }
}
