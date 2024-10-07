package com.twitter.servlets.models;

public class userAddDetailsModel {
    private Integer followersCount;
    private Integer followingCount;
    private Integer communitiesIn;
    private Integer tagsFollowed;
    private Integer postCount;

    public Integer getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(Integer followersCount) {
        this.followersCount = followersCount;
    }

    public Integer getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(Integer followingCount) {
        this.followingCount = followingCount;
    }

    public Integer getCommunitiesIn() {
        return communitiesIn;
    }

    public void setCommunitiesIn(Integer communitiesIn) {
        this.communitiesIn = communitiesIn;
    }

    public Integer getTagsFollowed() {
        return tagsFollowed;
    }

    public void setTagsFollowed(Integer tagsFollowed) {
        this.tagsFollowed = tagsFollowed;
    }

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }
}
