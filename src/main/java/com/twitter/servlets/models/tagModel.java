package com.twitter.servlets.models;

public class tagModel {
    private int tagId;

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        tagModel tag = (tagModel) o;
        return tagId == tag.tagId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(tagId);
    }
}
