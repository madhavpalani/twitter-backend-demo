package com.twitter.servlets.models;

public class mentionsModel {
    private int mentionId;
    private String mentionName;

    public int getMentionId() {
        return mentionId;
    }

    public void setMentionId(int mentionId) {
        this.mentionId = mentionId;
    }

    public String getMentionName() {
        return mentionName;
    }

    public void setMentionName(String mentionName) {
        this.mentionName = mentionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        mentionsModel that = (mentionsModel) o;
        // Mentions are considered equal if both mentionId and mentionName match
        return mentionId == that.mentionId && (mentionName != null ? mentionName.equals(that.mentionName) : that.mentionName == null);
    }

    // Override hashCode() to ensure consistency with equals()
    @Override
    public int hashCode() {
        int result = Integer.hashCode(mentionId);
        result = 31 * result + (mentionName != null ? mentionName.hashCode() : 0);
        return result;
    }
}
