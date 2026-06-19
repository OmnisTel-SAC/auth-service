package com.omnistel.authservice.service;

import com.omnistel.authservice.dto.*;
import com.omnistel.authservice.entity.Role;
import com.omnistel.authservice.entity.User;
import com.omnistel.authservice.exception.RateLimitExceededException;
import com.omnistel.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.jwt.access-token-expiration:86400000}")
    private long accessTokenExpiration;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final RateLimitService rateLimitService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: username={}, email={}", request.getUsername(), request.getEmail());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .role(Role.CLIENT)
            .build();

        user = userRepository.save(user);
        String token = generateToken(user);

        log.info("User registered successfully: id={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        if (rateLimitService.isBlocked(request.getUsername())) {
            log.warn("Login blocked due to rate limit: username={}", request.getUsername());
            throw new RateLimitExceededException(
                "Demasiados intentos fallidos. Intente de nuevo en 15 minutos.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            rateLimitService.resetAttempts(request.getUsername());

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String token = generateToken(user);
            log.info("User logged in: id={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
            return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        } catch (BadCredentialsException e) {
            rateLimitService.recordFailedAttempt(request.getUsername());
            throw e;
        }
    }

    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.fromEntity(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public AuthResponse registerByAdmin(String username, String email, String password,
                                         String firstName, String lastName, String phone, String roleStr) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr);
        }
        if (role == Role.CLIENT) {
            throw new IllegalArgumentException("Use register endpoint for CLIENT role");
        }

        User user = User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(password))
            .firstName(firstName)
            .lastName(lastName)
            .phone(phone)
            .role(role)
            .build();

        user = userRepository.save(user);
        String token = generateToken(user);

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }

    public List<UserResponse> getUsersByRole(String role) {
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        return userRepository.findByRole(roleEnum).stream()
            .map(UserResponse::fromEntity)
            .toList();
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("omnistel-auth")
            .issuedAt(now)
            .expiresAt(now.plusMillis(accessTokenExpiration))
            .subject(user.getId().toString())
            .claim("username", user.getUsername())
            .claim("role", user.getRole().name())
            .claim("email", user.getEmail())
            .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
