package com.zendapag.api.service;

import com.zendapag.api.dto.*;
import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.security.JwtTokenProvider;
import com.zendapag.core.entity.User;
import com.zendapag.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .cpfCnpj(request.getCpfCnpj())
                .build();

        User savedUser = userService.createUser(user);
        return UserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticate(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsernameOrEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            String token = tokenProvider.generateToken(authentication);
            User user = userService.findByUsernameOrEmail(request.getUsernameOrEmail());

            return LoginResponse.of(token, UserResponse.from(user));

        } catch (Exception ex) {
            log.error("Authentication failed for user: {}", request.getUsernameOrEmail(), ex);
            throw new BusinessException("Invalid credentials");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");

        if (!tokenProvider.validateToken(token)) {
            throw new BusinessException("Invalid token");
        }

        String username = tokenProvider.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        String newToken = tokenProvider.generateToken(username);

        return LoginResponse.of(newToken, UserResponse.from(user));
    }
}