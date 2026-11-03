package com.talent360.auth.controller;

import com.talent360.auth.dto.AuthResponse;
import com.talent360.auth.dto.LoginRequest;
import com.talent360.auth.dto.RefreshTokenRequest;
import com.talent360.auth.dto.RegisterRequest;
import com.talent360.auth.exception.InvalidTokenException;
import com.talent360.auth.service.AuthCookieService;
import com.talent360.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and JWT token refresh with HttpOnly cookies")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @Operation(summary = "Register a new user", description = "Creates an account with role ADMIN, RECRUITER, or CANDIDATE and sets HttpOnly refresh cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        ResponseCookie cookie = authCookieService.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @Operation(summary = "Authenticate user", description = "Authenticates user credentials, sets HttpOnly refresh cookie, and returns access token in JSON")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ResponseCookie cookie = authCookieService.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @Operation(summary = "Refresh JWT access token", description = "Rotates refresh token in HttpOnly cookie and returns a new access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "${app.jwt.cookie.name:refreshToken}", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        String token = cookieRefreshToken;
        if (token == null || token.isBlank()) {
            if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
                token = request.getRefreshToken();
            }
        }

        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Refresh token is missing from cookie or request body");
        }

        AuthResponse response = authService.refreshToken(token);
        ResponseCookie cookie = authCookieService.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @Operation(summary = "Logout user", description = "Clears the HttpOnly refresh token cookie")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie cleanCookie = authCookieService.createCleanRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(Map.of("message", "Successfully logged out. Refresh token cleared."));
    }
}
