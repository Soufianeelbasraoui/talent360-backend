package com.talent360.auth.service;

import com.talent360.auth.dto.AuthResponse;
import com.talent360.auth.dto.LoginRequest;
import com.talent360.auth.dto.RefreshTokenRequest;
import com.talent360.auth.dto.RegisterRequest;
import com.talent360.auth.entity.RefreshToken;
import com.talent360.auth.exception.EmailAlreadyExistsException;
import com.talent360.auth.exception.InvalidTokenException;
import com.talent360.auth.repository.RefreshTokenRepository;
import com.talent360.auth.util.TokenHashUtil;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.Role;
import com.talent360.security.JwtService;
import com.talent360.security.ratelimit.LoginRateLimiterService;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginRateLimiterService loginRateLimiterService;

    @Value("${app.jwt.access-token-expiration-ms:900000}") // 15 minutes default
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}") // 7 days default
    private long refreshTokenExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Enforce privilege separation: Public registration cannot self-assign ADMIN or RECRUITER
        if (request.getRole() != null && (request.getRole() == Role.ADMIN || request.getRole() == Role.RECRUITER)) {
            log.warn("Blocked unauthorized role assignment attempt: {} during public registration", request.getRole());
            throw new SecurityException("Unauthorized role assignment: Public registration only allows CANDIDATE role");
        }

        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String cleanEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new EmailAlreadyExistsException(cleanEmail);
        }

        User user = User.builder()
                .email(cleanEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CANDIDATE) // Public registration strictly defaults to CANDIDATE
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        Candidate candidate = Candidate.builder()
                .user(savedUser)
                .firstName(request.getFirstName() != null && !request.getFirstName().isBlank() ? request.getFirstName().trim() : "Candidate")
                .lastName(request.getLastName() != null && !request.getLastName().isBlank() ? request.getLastName().trim() : "User")
                .build();
        candidateRepository.save(candidate);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        // Store hashed refresh token in database with a new token family
        RefreshToken tokenEntity = RefreshToken.builder()
                .tokenHash(TokenHashUtil.hash(refreshToken))
                .user(savedUser)
                .familyId(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .build();
        refreshTokenRepository.save(tokenEntity);

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

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();

        // 1. Check brute-force rate limit
        loginRateLimiterService.checkRateLimit(cleanEmail);

        // 2. Authenticate credentials
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            loginRateLimiterService.recordFailedAttempt(cleanEmail);
            throw new BadCredentialsException("Invalid email or password");
        } catch (DisabledException | LockedException ex) {
            throw new DisabledException("Account is disabled or suspended");
        }

        // Reset rate limit on successful authentication
        loginRateLimiterService.resetAttempts(cleanEmail);

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new DisabledException("Account is disabled or suspended");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Store hashed refresh token in database with a new family ID
        RefreshToken tokenEntity = RefreshToken.builder()
                .tokenHash(TokenHashUtil.hash(refreshToken))
                .user(user)
                .familyId(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .build();
        refreshTokenRepository.save(tokenEntity);

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

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null) {
            throw new InvalidTokenException("Refresh token is required");
        }
        return refreshToken(request.getRefreshToken());
    }

    @Transactional
    public AuthResponse refreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        if (!jwtService.isRefreshToken(token)) {
            throw new InvalidTokenException("Invalid refresh token type");
        }

        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("User not found for provided token"));

        if (!user.isActive()) {
            throw new DisabledException("Account is disabled or suspended");
        }

        if (!jwtService.isTokenValid(token, user)) {
            throw new InvalidTokenException("Expired or invalid refresh token");
        }

        String tokenHash = TokenHashUtil.hash(token);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is unrecognized or revoked"));

        // Refresh Token Reuse Detection
        if (storedToken.isRevoked()) {
            refreshTokenRepository.revokeFamily(storedToken.getFamilyId(), LocalDateTime.now());
            log.warn("SECURITY ALERT: Refresh token reuse detected for family {}. Revoking all tokens for user {}",
                    storedToken.getFamilyId(), email);
            throw new InvalidTokenException("Refresh token was previously revoked. Session invalidated for security.");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Expired refresh token");
        }

        // Invalidate current token
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        // Rotate: generate new access token and new refresh token under the same family
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        RefreshToken newEntity = RefreshToken.builder()
                .tokenHash(TokenHashUtil.hash(newRefreshToken))
                .user(user)
                .familyId(storedToken.getFamilyId())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .build();
        refreshTokenRepository.save(newEntity);

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

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            String tokenHash = TokenHashUtil.hash(token);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.setRevoked(true);
                rt.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(rt);
                log.debug("Revoked refresh token on logout for user {}", rt.getUser() != null ? rt.getUser().getEmail() : "unknown");
            });
        }
    }
}
