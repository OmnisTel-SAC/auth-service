package com.omnistel.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistel.authservice.dto.*;
import com.omnistel.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
    excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void register_ShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@omnistel.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(authService.register(any(RegisterRequest.class)))
            .thenReturn(new AuthResponse("token", 1L, "newuser", "new@omnistel.com", "CLIENT"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void login_ShouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
            .thenReturn(new AuthResponse("token", 1L, "testuser", "test@omnistel.com", "CLIENT"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void getUsersByRole_ShouldReturn200() throws Exception {
        List<UserResponse> users = List.of(
            new UserResponse(1L, "admin", "admin@omnistel.com", "Admin", "User", "ADMIN")
        );

        when(authService.getUsersByRole("ADMIN")).thenReturn(users);

        mockMvc.perform(get("/api/auth/users")
                .param("role", "ADMIN")
                .header("X-Internal-Request", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username").value("admin"));
    }

    @Test
    void register_ShouldReturn400OnInvalidInput() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");
        request.setEmail("invalid-email");
        request.setPassword("12");
        request.setFirstName("");
        request.setLastName("");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
