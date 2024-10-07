package com.twitter.servlets.models;

public class replyLikeModel {
    private Integer ReplyID;
    private String LikeType;
    private Integer postID;

    public Integer getReplyID() {
        return ReplyID;
    }

    public void setReplyID(Integer replyID) {
        ReplyID = replyID;
    }

    public String getLikeType() {
        return LikeType;
    }

    public void setLikeType(String likeType) {
        LikeType = likeType;
    }

    public void setPostID(Integer postID) {
        this.postID = postID;
    }

    public Integer getPostID() {
        return postID;
    }
}
