package com.twitter.servlets.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class connectedAccount {
    @JsonProperty("type_to_connect")
    private String type_to_connect;
    @JsonProperty("account")
    private String account;

    public void setType_to_connect(String type_to_connect) {
        this.type_to_connect = type_to_connect;
    }

    public String getType_to_connect() {
        return type_to_connect;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }
}
