package com.omnistel.authservice.repository;

import com.omnistel.authservice.entity.Role;
import com.omnistel.authservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByUsername() {
        User user = User.builder()
            .username("testuser")
            .email("test@omnistel.com")
            .password("encoded")
            .firstName("Test")
            .lastName("User")
            .role(Role.CLIENT)
            .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals("test@omnistel.com", found.get().getEmail());
    }

    @Test
    void findByRole_ShouldReturnUsers() {
        User client = User.builder()
            .username("client1").email("client1@test.com")
            .password("pwd").role(Role.CLIENT).build();
        User admin = User.builder()
            .username("admin1").email("admin1@test.com")
            .password("pwd").role(Role.ADMIN).build();

        userRepository.save(client);
        userRepository.save(admin);

        List<User> admins = userRepository.findByRole(Role.ADMIN);
        assertEquals(1, admins.size());
        assertEquals("admin1", admins.get(0).getUsername());
    }

    @Test
    void existsByUsername_ShouldReturnTrue() {
        User user = User.builder()
            .username("existing")
            .email("existing@test.com")
            .password("pwd")
            .role(Role.CLIENT)
            .build();

        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("existing"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    void existsByEmail_ShouldReturnTrue() {
        User user = User.builder()
            .username("user")
            .email("exists@test.com")
            .password("pwd")
            .role(Role.CLIENT)
            .build();

        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("exists@test.com"));
    }
}
