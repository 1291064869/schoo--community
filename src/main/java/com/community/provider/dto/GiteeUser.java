package com.community.provider.dto;

import lombok.Data;

@Data
public class GiteeUser {
    private String name;
    private Long id;
    private String bio;  //描述
    private String avatarUrl;
}
