package com.talent360.auth.service;

import com.talent360.auth.dto.AuthResponse;
import com.talent360.auth.dto.LoginRequest;
import com.talent360.auth.dto.RegisterRequest;
import com.talent360.auth.entity.RefreshToken;
import com.talent360.auth.exception.EmailAlreadyExistsException;
import com.talent360.auth.exception.InvalidTokenException;
import com.talent360.auth.repository.RefreshTokenRepository;
import com.talent360.auth.util.TokenHashUtil;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.Role;
import com.talent360.security.JwtService;
import com.talent360.security.ratelimit.LoginRateLimiterService;
import com.talent360.security.ratelimit.RateLimitExceededException;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginRateLimiterService loginRateLimiterService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900000L); // 15 min
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L); // 7 days
    }

    @Test
    @DisplayName("Should successfully register candidate and hash refresh token in database")
    void registerCandidateSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .email("candidate@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .role(Role.CANDIDATE)
                .firstName("Sara")
                .lastName("Mansouri")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .email("candidate@example.com")
                .password("encoded_pass")
                .role(Role.CANDIDATE)
                .active(true)
                .build();

        when(userRepository.existsByEmail("candidate@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(savedUser)).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken(savedUser)).thenReturn("mock-refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getEmail()).isEqualTo("candidate@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CANDIDATE);

        verify(candidateRepository, times(1)).save(any());
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should block self-assigning ADMIN role during public registration")
    void registerUnauthorizedAdminRoleThrowsSecurityException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("attacker@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .role(Role.ADMIN)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Unauthorized role assignment");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should block self-assigning RECRUITER role during public registration")
    void registerUnauthorizedRecruiterRoleThrowsSecurityException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("attacker@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .role(Role.RECRUITER)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Unauthorized role assignment");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration when password and confirmPassword do not match")
    void registerPasswordMismatchThrowsIllegalArgumentException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .confirmPassword("Mismatch999!")
                .role(Role.CANDIDATE)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when registering duplicate email")
    void registerDuplicateEmailThrowsEmailAlreadyExistsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .role(Role.CANDIDATE)
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate valid credentials, reset rate limiter and store refresh token")
    void loginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .role(Role.CANDIDATE)
                .active(true)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token-xyz");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token-xyz");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-xyz");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-xyz");
        verify(loginRateLimiterService, times(1)).checkRateLimit("user@example.com");
        verify(loginRateLimiterService, times(1)).resetAttempts("user@example.com");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should reject login if account is deactivated or suspended")
    void loginDisabledAccountThrowsDisabledException() {
        LoginRequest request = LoginRequest.builder()
                .email("inactive@example.com")
                .password("Password123!")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("Account is disabled"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("Account is disabled or suspended");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should trigger rate limiter and record login failure on bad credentials")
    void loginBadCredentialsRecordsFailure() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("WrongPass!")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginRateLimiterService, times(1)).checkRateLimit("user@example.com");
        verify(loginRateLimiterService, times(1)).recordFailedAttempt("user@example.com");
    }

    @Test
    @DisplayName("Should block login if rate limiter threshold is exceeded")
    void loginRateLimiterBlocksExcessiveFailedAttempts() {
        LoginRequest request = LoginRequest.builder()
                .email("targeted@example.com")
                .password("Password123!")
                .build();

        doThrow(new RateLimitExceededException("Too many login attempts", 900))
                .when(loginRateLimiterService).checkRateLimit("targeted@example.com");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Too many login attempts");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should successfully rotate refresh token and revoke previous token")
    void refreshTokenRotationSuccess() {
        String oldRefreshToken = "valid-old-refresh-token";
        User user = User.builder()
                .id(20L)
                .email("candidate@example.com")
                .role(Role.CANDIDATE)
                .active(true)
                .build();

        RefreshToken existingToken = RefreshToken.builder()
                .id(1L)
                .familyId("family-uuid-1")
                .tokenHash(TokenHashUtil.hash(oldRefreshToken))
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(jwtService.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtService.extractUsername(oldRefreshToken)).thenReturn("candidate@example.com");
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(oldRefreshToken, user)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hash(oldRefreshToken))).thenReturn(Optional.of(existingToken));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-rotated-refresh-token");

        AuthResponse response = authService.refreshToken(oldRefreshToken);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-rotated-refresh-token");
        assertThat(existingToken.isRevoked()).isTrue();

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should detect refresh token reuse and revoke the entire token family")
    void refreshTokenReuseDetectionRevokesFamily() {
        String stolenOrReplayedToken = "replayed-refresh-token";
        User user = User.builder()
                .id(20L)
                .email("candidate@example.com")
                .role(Role.CANDIDATE)
                .active(true)
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .familyId("compromised-family-123")
                .tokenHash(TokenHashUtil.hash(stolenOrReplayedToken))
                .user(user)
                .revoked(true) // Already used/revoked
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(jwtService.isRefreshToken(stolenOrReplayedToken)).thenReturn(true);
        when(jwtService.extractUsername(stolenOrReplayedToken)).thenReturn("candidate@example.com");
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(stolenOrReplayedToken, user)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hash(stolenOrReplayedToken))).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refreshToken(stolenOrReplayedToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("previously revoked");

        verify(refreshTokenRepository, times(1)).revokeFamily(eq("compromised-family-123"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should revoke token in repository on logout")
    void logoutRevokesRefreshToken() {
        String tokenToLogout = "valid-refresh-token";
        RefreshToken storedToken = RefreshToken.builder()
                .id(5L)
                .tokenHash(TokenHashUtil.hash(tokenToLogout))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hash(tokenToLogout))).thenReturn(Optional.of(storedToken));

        authService.logout(tokenToLogout);

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(1)).save(storedToken);
    }
}
