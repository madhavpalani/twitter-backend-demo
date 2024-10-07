package com.twitter.servlets.models;

public class postLikeModel {
    private Integer PostID;
    private String LikeType;

    public void setPostID(Integer postID) {
        PostID = postID;
    }

    public Integer getPostID() {
        return PostID;
    }

    public String getLikeType() {
        return LikeType;
    }

    public void setLikeType(String likeType) {
        LikeType = likeType;
    }
}
