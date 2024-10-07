package com.twitter.servlets.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class followModel {
    @JsonProperty("followId")
    private int followId;

    public void setFollow_id(int followid) {
        this.followId = followid;
    }

    public int getFollow_id() {
        return followId;
    }
}
