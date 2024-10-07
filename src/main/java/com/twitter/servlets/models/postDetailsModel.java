package com.twitter.servlets.models;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
@JsonPropertyOrder({
        "text",
        "timestamp",
        "parentPostId",
        "communityId",
        "delegatedId",
        "likes",
        "reposts",
        "postTags",
        "postMentions",
})

public class postDetailsModel {
    private String text;
    private Integer ParentPostId;
    private Integer CommunityId;
    private Integer DelegatedId;
    private Integer Likes =0;
    private Integer Reposts =0;
    private Timestamp timestamp;
    private HashSet<tagModel> PostTags;
    private HashSet<mentionsModel> PostMentions;

    public postDetailsModel() {
        this.PostTags = new HashSet<>();
        this.PostMentions = new HashSet<>();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getParentPostId() {
        return ParentPostId;
    }

    public void setParentPostId(Integer parentPostId) {
        ParentPostId = parentPostId;
    }

    public Integer getDelegatedId() {
        return DelegatedId;
    }

    public void setDelegatedId(Integer delegatedId) {
        DelegatedId = delegatedId;
    }

    public Integer getCommunityId() {
        return CommunityId;
    }

    public void setCommunityId(Integer communityId) {
        CommunityId = communityId;
    }


    public void setLikes(Integer likes) {
        this.Likes = likes;
    }

    public Integer getLikes() {
        return Likes;
    }

    public void setReposts(Integer reposts) {
        Reposts = reposts;
    }

    public Integer getReposts() {
        return Reposts;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    public void setPostTags(HashSet<tagModel> postTags) {
        PostTags = postTags;
    }

    public HashSet<tagModel> getPostTags() {
        return PostTags;
    }

    public HashSet<mentionsModel> getPostMentions() {
        return PostMentions;
    }

    public void setPostMentions(HashSet<mentionsModel> postMentions) {
        PostMentions = postMentions;
    }

    public void addTagId(int tagId) {
        tagModel tag = new tagModel();
        tag.setTagId(tagId);
        PostTags.add(tag);
    }

    public void addMentionID(int mentionID){
        mentionsModel mentions = new mentionsModel();
        mentions.setMentionId(mentionID);
        PostMentions.add(mentions);
    }

    public void addMentionNames(String mentionName){
        mentionsModel mentions = new mentionsModel();
        mentions.setMentionName(mentionName);
        PostMentions.add(mentions);
    }
}

