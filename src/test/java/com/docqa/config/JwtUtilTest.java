package com.docqa.config;

import com.docqa.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-for-unit-testing-only-must-be-long");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateToken_ShouldReturnNonNullToken() {
        User user = User.builder().username("testuser").build();
        String token = jwtUtil.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        User user = User.builder().username("testuser").build();
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        User user = User.builder().username("testuser").password("pass")
                .role(User.Role.USER).build();
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.validateToken(token, user)).isTrue();
    }

    @Test
    void validateToken_ShouldReturnFalse_ForWrongUser() {
        User user1 = User.builder().username("user1").password("p").role(User.Role.USER).build();
        User user2 = User.builder().username("user2").password("p").role(User.Role.USER).build();
        String token = jwtUtil.generateToken(user1);
        assertThat(jwtUtil.validateToken(token, user2)).isFalse();
    }
}
