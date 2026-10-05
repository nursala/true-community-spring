package com.truecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * MVC form backing comment submission from the arena page.
 */
public class CommentForm {

    @NotBlank
    @Size(max = 1000)
    private String content;

    public CommentForm() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
