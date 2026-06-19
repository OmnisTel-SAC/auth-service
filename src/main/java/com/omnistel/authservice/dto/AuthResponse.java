package com.omnistel.authservice.dto;

import lombok.Data;

@Data
public class AuthResponse {

    private final String token;
    private final String type = "Bearer";
    private final Long userId;
    private final String username;
    private final String email;
    private final String role;

    public AuthResponse(String token, Long userId, String username, String email, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}
