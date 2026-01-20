package com.example.bankcards.service.impl;

import com.example.bankcards.dto.authentication.AuthResponse;
import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceExistsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtProvider;
import com.example.bankcards.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        String password = registerRequest.getPassword();
        String confirmPassword = registerRequest.getConfirmPassword();

        if (!password.equals(confirmPassword)) {
            throw BusinessException.passwordsDoNotMatch();
        }

        String username = registerRequest.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw ResourceExistsException.username(username);
        }

        String email = registerRequest.getEmail();
        if (userRepository.existsByEmail(email)) {
            throw ResourceExistsException.email(email);
        }

        String encryptedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(encryptedPassword)
                .build();

        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> ResourceNotFoundException.role(RoleType.USER));
        user.getRoles().add(userRole);

        userRepository.save(user);

        String token = jwtProvider.generateToken(user);

        UserDto userDto = userMapper.toDto(user);

        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtProvider.getTokenExpiration())
                .user(userDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        User user = userRepository.findByUsername(username)
                .orElseThrow(AuthException::invalidCredentials);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw AuthException.invalidCredentials();
        }

        String token = jwtProvider.generateToken(user);
        UserDto userDto = userMapper.toDto(user);

        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtProvider.getTokenExpiration())
                .user(userDto)
                .build();
    }
}
