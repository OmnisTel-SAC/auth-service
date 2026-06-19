package com.omnistel.authservice.service;

import com.omnistel.authservice.exception.RateLimitExceededException;
import com.omnistel.authservice.dto.AuthResponse;
import com.omnistel.authservice.dto.LoginRequest;
import com.omnistel.authservice.dto.RegisterRequest;
import com.omnistel.authservice.dto.UserResponse;
import com.omnistel.authservice.entity.Role;
import com.omnistel.authservice.entity.User;
import com.omnistel.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtEncoder jwtEncoder;
    @Mock private RateLimitService rateLimitService;

    private AuthService authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtEncoder, rateLimitService);

        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@omnistel.com")
            .password("encodedPassword")
            .firstName("Test")
            .lastName("User")
            .role(Role.CLIENT)
            .enabled(true)
            .build();
    }

    @Test
    void register_ShouldCreateClientUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@omnistel.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@omnistel.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mock(Jwt.class));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@omnistel.com", response.getEmail());
        assertEquals("CLIENT", response.getRole());
        verify(userRepository).save(argThat(user -> user.getRole() == Role.CLIENT));
    }

    @Test
    void register_ShouldThrowWhenUsernameTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setEmail("new@omnistel.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowWhenEmailTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("taken@omnistel.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@omnistel.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldSucceed() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(rateLimitService.isBlocked("testuser")).thenReturn(false);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mock(Jwt.class));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        verify(rateLimitService).resetAttempts("testuser");
    }

    @Test
    void login_ShouldThrowOnBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong");

        when(rateLimitService.isBlocked("testuser")).thenReturn(false);
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(rateLimitService).recordFailedAttempt("testuser");
    }

    @Test
    void login_ShouldThrowWhenRateLimited() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(rateLimitService.isBlocked("testuser")).thenReturn(true);

        assertThrows(RateLimitExceededException.class, () -> authService.login(request));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void getUsersByRole_ShouldReturnUsers() {
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(testUser));

        List<UserResponse> result = authService.getUsersByRole("ADMIN");

        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    void getUsersByRole_ShouldThrowOnInvalidRole() {
        assertThrows(IllegalArgumentException.class, () -> authService.getUsersByRole("INVALID"));
    }

    @Test
    void getCurrentUser_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserResponse result = authService.getCurrentUser("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getCurrentUser_ShouldThrowWhenNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.getCurrentUser("unknown"));
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = authService.getUserById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void registerByAdmin_ShouldCreateAgentOrAdmin() {
        User agentUser = User.builder()
            .id(2L)
            .username("agent1")
            .email("agent@omnistel.com")
            .password("encoded")
            .firstName("Agent")
            .lastName("One")
            .role(Role.AGENT)
            .build();

        when(userRepository.existsByUsername("agent1")).thenReturn(false);
        when(userRepository.existsByEmail("agent@omnistel.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(agentUser);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mock(Jwt.class));

        AuthResponse response = authService.registerByAdmin("agent1", "agent@omnistel.com",
            "password123", "Agent", "One", null, "AGENT");

        assertNotNull(response);
        verify(userRepository).save(argThat(user -> user.getRole() == Role.AGENT));
    }

    @Test
    void registerByAdmin_ShouldThrowOnInvalidRole() {
        assertThrows(IllegalArgumentException.class, () ->
            authService.registerByAdmin("u", "e@m.com", "pass", "F", "L", null, "INVALID"));
    }

    @Test
    void registerByAdmin_ShouldThrowOnClientRole() {
        assertThrows(IllegalArgumentException.class, () ->
            authService.registerByAdmin("u", "e@m.com", "pass", "F", "L", null, "CLIENT"));
    }
}
