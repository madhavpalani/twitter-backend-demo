package com.twitter.servlets.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
@JsonPropertyOrder({
        "name",
        "place",
        "link",
        "professionalAccOrNot",
        "phoneNo",
        "countryLiving",
        "email",
        "verifiedOrNot",
        "pass",
        "twoFactorAuth",
        "protectPost",
        "protectVideo",
        "photoTagging",
        "directMsg",
        "allowCalls",
        "PersonalizeByLocation",
        "readReceipts",
        "findViaEmail",
        "findViaPhone",
        "personalizeByAds",
        "personalizeById",
        "infShareWithBusiPartners",
        "allowPostsWithGrok",
        "accountsConnected"
})

public class userModel {
    private String name = "";
    private String place = "";
    private String link = "";
    private boolean professionalAccOrNot = false;
    private String phoneNo = "";
    private String countryLiving = "";
    private String email = "";
    private boolean verifiedOrNot = false;
    private String pass = "";
    private boolean twoFactorAuth = false;
    private List<connectedAccount> accountsConnected = new ArrayList<>();  // Assuming accounts_connected is a list of strings
    private boolean protectPost = false;
    private boolean protectVideo = false;
    private boolean photoTagging = false;
    private boolean directMsg = false;
    private boolean allowCalls = false;
    private boolean readReceipts = false;
    private boolean findViaEmail = false;
    private boolean findViaPhone = false;
    private boolean personalizedAds = false;
    private boolean personalizeById = false;
    private boolean infShareWithBusiPartners = false;
    private boolean PersonalizeByLocation = false;
    private boolean allowPostsWithGrok = false;

    public userModel() {
        this.accountsConnected=new ArrayList<>();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isProfessionalAccOrNot() {
        return professionalAccOrNot;
    }

    public void setProfessionalAccOrNot(boolean professionalAccOrNot) {
        this.professionalAccOrNot = professionalAccOrNot;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getCountryLiving() {
        return countryLiving;
    }

    public void setCountryLiving(String countryLiving) {
        this.countryLiving = countryLiving;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVerifiedOrNot() {
        return verifiedOrNot;
    }

    public void setVerifiedOrNot(boolean verifiedOrNot) {
        this.verifiedOrNot = verifiedOrNot;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean isTwoFactorAuth() {
        return twoFactorAuth;
    }

    public void setTwoFactorAuth(boolean twoFactorAuth) {
        this.twoFactorAuth = twoFactorAuth;
    }

    public List<connectedAccount> getAccountsConnected() {
        return accountsConnected;
    }

    public void setAccountsConnected(List<connectedAccount> accountsConnected) {
        this.accountsConnected = accountsConnected;
    }

    public void setProtectPost(boolean protectPost) {
        this.protectPost = protectPost;
    }

    public boolean isProtectPost() {
        return protectPost;
    }

    public void setProtectVideo(boolean protectVideo) {
        this.protectVideo = protectVideo;
    }

    public boolean isProtectVideo() {
        return protectVideo;
    }

    public boolean isPhotoTagging() {
        return photoTagging;
    }

    public void setPhotoTagging(boolean photoTagging) {
        this.photoTagging = photoTagging;
    }

    public boolean isDirectMsg() {
        return directMsg;
    }

    public void setDirectMsg(boolean directMsg) {
        this.directMsg = directMsg;
    }

    public boolean isAllowCalls() {
        return allowCalls;
    }

    public void setAllowCalls(boolean allowCalls) {
        this.allowCalls = allowCalls;
    }

    public boolean isReadReceipts() {
        return readReceipts;
    }

    public void setReadReceipts(boolean readReceipts) {
        this.readReceipts = readReceipts;
    }

    public boolean isFindViaEmail() {
        return findViaEmail;
    }

    public void setFindViaEmail(boolean findViaEmail) {
        this.findViaEmail = findViaEmail;
    }

    public boolean isFindViaPhone() {
        return findViaPhone;
    }

    public void setFindViaPhone(boolean findViaPhone) {
        this.findViaPhone = findViaPhone;
    }

    public boolean isPersonalizedAds() {
        return personalizedAds;
    }

    public void setPersonalizedAds(boolean personalizedAds) {
        this.personalizedAds = personalizedAds;
    }

    public boolean isPersonalizeById() {
        return personalizeById;
    }

    public void setPersonalizeById(boolean personalizeById) {
        this.personalizeById = personalizeById;
    }

    public boolean isPersonalizeByLocation() {
        return PersonalizeByLocation;
    }

    public void setPersonalizeByLocation(boolean personalizeByLocation) {
        PersonalizeByLocation = personalizeByLocation;
    }

    public boolean isInfShareWithBusiPartners() {
        return infShareWithBusiPartners;
    }

    public void setInfShareWithBusiPartners(boolean infShareWithBusiPartners) {
        this.infShareWithBusiPartners = infShareWithBusiPartners;
    }

    public boolean isAllowPostsWithGrok() {
        return allowPostsWithGrok;
    }

    public void setAllowPostsWithGrok(boolean allowPostsWithGrok) {
        this.allowPostsWithGrok = allowPostsWithGrok;
    }

    public void addConnectedAccounts(String type_to_connect, String account){
        connectedAccount ca = new connectedAccount();
        ca.setType_to_connect(type_to_connect);
        ca.setAccount(account);
        accountsConnected.add(ca);
    }
}

