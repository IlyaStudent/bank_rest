package com.example.bankcards.service;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.RoleType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDto createUser(@NotNull CreateUserRequest createUserRequest);

    UserDto getUserById(@NotNull Long userId);

    UserDto getUserByUsername(@NotNull String username);

    Page<UserDto> getAllUsers(Pageable pageable);

    UserDto updateUser(@NotNull Long userId, @NotNull UpdateUserRequest updateUserRequest);

    void deleteUser(@NotNull Long userId);

    UserDto assignRole(@NotNull Long userId, @NotNull RoleType role);
}
