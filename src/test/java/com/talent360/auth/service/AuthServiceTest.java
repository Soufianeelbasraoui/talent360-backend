package com.talent360.auth.service;

import com.talent360.auth.dto.AuthResponse;
import com.talent360.auth.dto.LoginRequest;
import com.talent360.auth.dto.RegisterRequest;
import com.talent360.auth.exception.EmailAlreadyExistsException;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.Role;
import com.talent360.security.JwtService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 1800000L);
    }

    @Test
    @DisplayName("Should successfully register a candidate and generate JWT tokens")
    void registerCandidateSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .email("candidate@example.com")
                .password("Password123")
                .role(Role.CANDIDATE)
                .firstName("John")
                .lastName("Doe")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .email("candidate@example.com")
                .password("encoded_pass")
                .role(Role.CANDIDATE)
                .active(true)
                .build();

        when(userRepository.existsByEmail("candidate@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");
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
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when registering with duplicate email")
    void registerDuplicateEmailThrows() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Password123")
                .role(Role.RECRUITER)
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate and return tokens on valid login")
    void loginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("secret123")
                .build();

        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .role(Role.RECRUITER)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token-xyz");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token-xyz");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-xyz");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-xyz");
        assertThat(response.getRole()).isEqualTo(Role.RECRUITER);
    }

    @Test
    @DisplayName("Should successfully rotate refresh token and issue new access token")
    void refreshTokenRotationSuccess() {
        String oldRefreshToken = "valid-old-refresh-token";
        User user = User.builder()
                .id(20L)
                .email("candidate@example.com")
                .role(Role.CANDIDATE)
                .active(true)
                .build();

        when(jwtService.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtService.extractUsername(oldRefreshToken)).thenReturn("candidate@example.com");
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(oldRefreshToken, user)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-rotated-refresh-token");

        AuthResponse response = authService.refreshToken(oldRefreshToken);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-rotated-refresh-token");
        assertThat(response.getEmail()).isEqualTo("candidate@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CANDIDATE);
    }
}
