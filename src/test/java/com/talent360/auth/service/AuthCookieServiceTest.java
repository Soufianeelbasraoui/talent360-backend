package com.talent360.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    private AuthCookieService authCookieService;

    @BeforeEach
    void setUp() {
        authCookieService = new AuthCookieService();
        ReflectionTestUtils.setField(authCookieService, "cookieName", "refreshToken");
        ReflectionTestUtils.setField(authCookieService, "cookiePath", "/api/auth");
        ReflectionTestUtils.setField(authCookieService, "secure", false);
        ReflectionTestUtils.setField(authCookieService, "sameSite", "Lax");
        ReflectionTestUtils.setField(authCookieService, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    @DisplayName("Should create HttpOnly cookie with configured attributes")
    void createRefreshTokenCookie() {
        ResponseCookie cookie = authCookieService.createRefreshTokenCookie("sample-jwt-token");

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("sample-jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(604800L);
    }

    @Test
    @DisplayName("Should create clear cookie with maxAge=0 for logout")
    void createCleanRefreshTokenCookie() {
        ResponseCookie cookie = authCookieService.createCleanRefreshTokenCookie();

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(0L);
    }
}

