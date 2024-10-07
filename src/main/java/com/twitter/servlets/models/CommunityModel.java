package com.twitter.servlets.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommunityModel {
    @JsonProperty("CommunityName")
    private String CommunityName = "";
    @JsonProperty("About")
    private String About = "";
    @JsonProperty("Type")
    private String Type = "";

    public String getCommunityName() {
        return CommunityName;
    }

    public void setCommunityName(String communityName) {
        CommunityName = communityName;
    }

    public String getAbout() {
        return About;
    }

    public void setAbout(String about) {
        About = about;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }
}
