package com.talent360.user.service;

import com.talent360.common.dto.PageResponse;
import com.talent360.user.dto.UpdateUserRequest;
import com.talent360.user.dto.UserResponse;
import com.talent360.user.entity.User;
import com.talent360.user.mapper.UserMapper;
import com.talent360.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = getUserByEmail(email);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(String email, UpdateUserRequest request) {
        User user = getUserByEmail(email);
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            log.info("Password updated for user: {}", email);
        }
        User updated = userRepository.save(user);
        return userMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);
        return PageResponse.from(usersPage, userMapper::toResponse);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setActive(active);
        log.info("Updated user status for ID {} to active={}", id, active);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user with ID {}", id);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }
}
