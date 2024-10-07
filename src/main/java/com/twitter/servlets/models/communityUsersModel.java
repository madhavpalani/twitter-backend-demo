package com.twitter.servlets.models;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.twitter.servlets.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class communityUsersModel {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer communityId;

    private List<User> users;

    public communityUsersModel() {
        this.users = new ArrayList<>();
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }

    public Integer getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Integer communityId) {
        this.communityId = communityId;
    }
    public void addUserName(String userName){
        User u1 = new User();
        u1.setUserName(userName);
        users.add(u1);
    }
}
