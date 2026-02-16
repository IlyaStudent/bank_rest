package com.example.bankcards.service.impl;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
        log.debug("Creating user: username='{}'", createUserRequest.getUsername());

        validateUniqueUsername(createUserRequest.getUsername(), null);
        validateUniqueEmail(createUserRequest.getEmail(), null);

        User user = userMapper.createUser(createUserRequest);
        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        setUserRoles(user, createUserRequest.getRoles());

        user = userRepository.save(user);

        log.info("User created: id={}, username='{}'", user.getId(), user.getUsername());

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        return userMapper.toDto(findUserById(userId));
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
    public UserDto updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        log.debug("Updating user id={}", userId);

        User user = findUserById(userId);

        updateUsername(user, updateUserRequest.getUsername());
        updateEmail(user, updateUserRequest.getEmail());
        updatePassword(user, updateUserRequest.getPassword());
        updateRoles(user, updateUserRequest.getRoles());

        user = userRepository.save(user);

        log.info("User updated: id={}", userId);

        return userMapper.toDto(user);
    }

    @Override
    public void deleteUser(Long userId) {
        log.debug("Deleting user id={}", userId);

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.user(userId);
        }
        userRepository.deleteById(userId);

        log.info("User deleted: id={}", userId);
    }

    @Override
    public UserDto assignRole(Long userId, RoleType roleType) {
        log.debug("Assigning role: userId={}, role={}", userId, roleType);

        User user = findUserById(userId);
        Role role = findRole(roleType);
        user.getRoles().add(role);

        user = userRepository.save(user);

        log.info("Role assigned: userId={}, role={}", userId, roleType);

        return userMapper.toDto(user);
    }

    // --- Lookup --- //

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));
    }

    private Role findRole(RoleType roleType) {
        return roleRepository.findByName(roleType)
                .orElseThrow(() -> ResourceNotFoundException.role(roleType));
    }

    // --- Validation --- //

    private void validateUniqueUsername(String newUsername, String currentUsername) {
        Boolean userExists = userRepository.existsByUsername(newUsername);
        if (newUsername != null && !newUsername.equals(currentUsername) && userExists) {
            throw ResourceExistsException.username(newUsername);
        }
    }

    private void validateUniqueEmail(String newEmail, String currentEmail) {
        Boolean userExists = userRepository.existsByEmail(newEmail);
        if (newEmail != null && !newEmail.equals(currentEmail) && userExists) {
            throw ResourceExistsException.email(newEmail);
        }
    }

    // --- Field updates --- //

    private void updateUsername(User user, String newUsername) {
        if (newUsername != null && !newUsername.isBlank()) {
            validateUniqueUsername(newUsername, user.getUsername());
            user.setUsername(newUsername);
        }
    }

    private void updateEmail(User user, String newEmail) {
        if (newEmail != null && !newEmail.isBlank()) {
            validateUniqueEmail(newEmail, user.getEmail());
            user.setEmail(newEmail);
        }
    }

    private void updatePassword(User user, String newPassword) {
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
    }

    private void updateRoles(User user, Set<String> requestedRoles) {
        if (requestedRoles != null && !requestedRoles.isEmpty()) {
            setUserRoles(user, requestedRoles);
        }
    }

    // --- Role management --- //

    private void setUserRoles(User user, Set<String> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            user.getRoles().add(findRole(RoleType.USER));
        } else {
            user.setRoles(resolveRoles(requestedRoles));
        }
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(this::parseRoleType)
                .map(this::findRole)
                .collect(Collectors.toSet());
    }

    private RoleType parseRoleType(String roleName) {
        try {
            return RoleType.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalidRole(roleName);
        }
    }
}
