package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsdCreatedComment {
    public Long commentId;
    public String self;

    @JsonProperty("id")
    private void mapCommentId(String id) {
        this.commentId = Long.parseLong(id);
    }
}