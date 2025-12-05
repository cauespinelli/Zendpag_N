package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.User;
import com.zendapag.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(User user) {
        log.info("Creating user with username: {}", user.getUsername());

        validateUserData(user);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(User.UserStatus.ACTIVE);

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of(User.Role.USER));
        }

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional(readOnly = true)
    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username/email", usernameOrEmail));
    }

    @Transactional
    public User updateUserStatus(Long id, User.UserStatus status) {
        User user = findById(id);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private void validateUserData(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException("DUPLICATE_USERNAME", "Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email already exists");
        }

        if (user.getPassword() == null || user.getPassword().length() < 8) {
            throw new BusinessException("WEAK_PASSWORD", "Password must be at least 8 characters");
        }
    }
}
