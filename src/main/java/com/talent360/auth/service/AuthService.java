package com.talent360.auth.service;

import com.talent360.auth.dto.AuthResponse;
import com.talent360.auth.dto.LoginRequest;
import com.talent360.auth.dto.RefreshTokenRequest;
import com.talent360.auth.dto.RegisterRequest;
import com.talent360.auth.exception.EmailAlreadyExistsException;
import com.talent360.auth.exception.InvalidTokenException;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.Role;
import com.talent360.security.JwtService;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.access-token-expiration-ms:1800000}")
    private long accessTokenExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        if (request.getRole() == Role.CANDIDATE) {
            Candidate candidate = Candidate.builder()
                    .user(savedUser)
                    .firstName(request.getFirstName() != null ? request.getFirstName() : "Candidate")
                    .lastName(request.getLastName() != null ? request.getLastName() : "User")
                    .build();
            candidateRepository.save(candidate);
        }

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        log.info("Registered new user with ID: {} and role: {}", savedUser.getId(), savedUser.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(accessTokenExpiration)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new InvalidTokenException("Invalid email or credentials"));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(accessTokenExpiration)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null) {
            throw new InvalidTokenException("Refresh token is required");
        }
        return refreshToken(request.getRefreshToken());
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        if (!jwtService.isRefreshToken(token)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("User not found for provided token"));

        if (!jwtService.isTokenValid(token, user)) {
            throw new InvalidTokenException("Expired or invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        log.debug("Successfully refreshed and rotated tokens for user: {}", email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(accessTokenExpiration)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
