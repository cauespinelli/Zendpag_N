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
    public User createUser {
        log.info);

        validateUserData;

        user.setPassword));
        user.setStatus;

        if  == null || user.getRoles().isEmpty()) {
            user.setRoles);
        }

        User savedUser = userRepository.save;
        log.info);

        return savedUser;
    }

    @Transactional
    public User findById {
        return userRepository.findById
                .orElseThrow -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional
    public User findByUsername {
        return userRepository.findByUsername
                .orElseThrow -> new ResourceNotFoundException("User", "username", username));
    }

    @Transactional
    public User findByEmail {
        return userRepository.findByEmail
                .orElseThrow -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional
    public User findByUsernameOrEmail {
        return userRepository.findByUsernameOrEmail
                .orElseThrow -> new ResourceNotFoundException("User", "username/email", usernameOrEmail));
    }

    @Transactional
    public User updateUserStatus {
        User user = findById;
        user.setStatus;
        return userRepository.save;
    }

    @Transactional
    public boolean validatePassword {
        return passwordEncoder.matches;
    }

    private void validateUserData {
        if )) {
            throw new BusinessException);
        }

        if )) {
            throw new BusinessException);
        }

        if )) {
            throw new BusinessException);
        }
    }
}