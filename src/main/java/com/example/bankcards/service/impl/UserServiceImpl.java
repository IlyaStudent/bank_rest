package com.example.bankcards.service.impl;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceExistsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserDto createUser(CreateUserRequest createUserRequest) {
        String username = createUserRequest.getUsername();
        String email = createUserRequest.getEmail();
        String password = createUserRequest.getPassword();

        if (userRepository.existsByUsername(username)) {
            throw ResourceExistsException.username(username);
        }

        if (userRepository.existsByEmail(email)) {
            throw ResourceExistsException.email(email);
        }

        User user = userMapper.createUser(createUserRequest);
        user.setPassword(passwordEncoder.encode(password));

        Set<String> requestedRoles = createUserRequest.getRoles();

        setUserRoles(requestedRoles, user);

        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.userByUsername(username));

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository
                .findAll(pageable)
                .map(userMapper::toDto);
    }

    @Override
    public UserDto updateUser(Long userId, CreateUserRequest createUserRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));

        String newUsername = createUserRequest.getUsername();
        String newEmail = createUserRequest.getEmail();
        String newPassword = createUserRequest.getPassword();

        String oldUsername = user.getUsername();
        String oldEmail = user.getEmail();

        if (!oldUsername.equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            throw ResourceExistsException.username(newUsername);
        }

        if (!oldEmail.equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw ResourceExistsException.email(newEmail);
        }

        user.setUsername(newUsername);
        user.setEmail(newEmail);

        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        user = userRepository.save(user);

        return userMapper.toDto(user);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.user(userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public UserDto assignRole(Long userId, RoleType roleType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> ResourceNotFoundException.role(roleType));

        user.getRoles().add(role);

        user = userRepository.save(user);

        return userMapper.toDto(user);
    }

    private void setUserRoles(Set<String> requestedRoles, User user) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            Role defaultRole = roleRepository.findByName(RoleType.USER)
                    .orElseThrow(() -> ResourceNotFoundException.role(RoleType.USER));
            user.getRoles().add(defaultRole);
        } else {
            Set<Role> roles = requestedRoles
                    .stream()
                    .map(roleName -> {
                        RoleType roleType;
                        try {
                            roleType = RoleType.valueOf(roleName);
                        } catch (IllegalArgumentException e) {
                            throw BusinessException.invalidRole(roleName);
                        }

                        return roleRepository.findByName(roleType)
                                .orElseThrow(() -> ResourceNotFoundException.role(roleType));
                    })
                    .collect(Collectors.toSet());

            user.setRoles(roles);
        }
    }
}
