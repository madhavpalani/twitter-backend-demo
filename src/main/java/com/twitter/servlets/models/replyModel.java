package com.twitter.servlets.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.sql.Timestamp;
import java.util.HashSet;

@JsonPropertyOrder({
        "text",
        "PostId",
        "likes",
        "timestamp",
        "CommunityID",
        "DelegatedID",
        "ReplyTags",
        "ReplyMentions"
})

public class replyModel {
    private String text;
    private Integer PostId;
    private Integer likes;
    private Timestamp timestamp;
    private Integer CommunityID;
    private Integer DelegatedID;
    private HashSet<tagModel> ReplyTags;
    private HashSet<mentionsModel> ReplyMentions;
    public replyModel(){
        this.ReplyTags = new HashSet<>();
        this.ReplyMentions = new HashSet<>();
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setPostId(Integer postId) {
        PostId = postId;
    }

    public Integer getPostId() {
        return PostId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public void setReplyTags(HashSet<tagModel> replyTags) {
        ReplyTags = replyTags;
    }

    public HashSet<tagModel> getReplyTags() {
        return ReplyTags;
    }

    public HashSet<mentionsModel> getReplyMentions() {
        return ReplyMentions;
    }

    public void setReplyMentions(HashSet<mentionsModel> replyMentions) {
        ReplyMentions = replyMentions;
    }

    public Integer getDelegatedID() {
        return DelegatedID;
    }

    public void setDelegatedID(Integer delegatedID) {
        DelegatedID = delegatedID;
    }

    public Integer getCommunityID() {
        return CommunityID;
    }

    public void setCommunityID(Integer communityID) {
        CommunityID = communityID;
    }

    public void addTagID(int tagID){
        tagModel tags = new tagModel();
        tags.setTagId(tagID);
        ReplyTags.add(tags);
    }

    public void addMentionsName(int mentionID, String mentionName){
        mentionsModel mentions = new mentionsModel();
        mentions.setMentionId(mentionID);
        mentions.setMentionName(mentionName);
        ReplyMentions.add(mentions);
    }
}
