package com.talent360.auth.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Getter
public class AuthCookieService {

    @Value("${app.jwt.cookie.name:refreshToken}")
    private String cookieName;

    @Value("${app.jwt.cookie.path:/api/auth}")
    private String cookiePath;

    @Value("${app.jwt.cookie.secure:false}")
    private boolean secure;

    @Value("${app.jwt.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie createCleanRefreshTokenCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ZERO)
                .sameSite(sameSite)
                .build();
    }
}

