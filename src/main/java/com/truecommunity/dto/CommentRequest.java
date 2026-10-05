package com.truecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON request body for creating a statement comment through the REST API.
 */
public class CommentRequest {

    @NotBlank
    @Size(max = 1000)
    private String content;

    public CommentRequest() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
