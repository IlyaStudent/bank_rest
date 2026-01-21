package com.example.bankcards.controller;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("${end.point.users}")
public class UserController {
    private final UserService userService;

    @GetMapping
    public Page<UserDto> getAllUsers(
            Pageable pageable
    ) {
        return userService.getAllUsers(pageable);
    }

    @GetMapping("${end.point.id}")
    public UserDto getUser(
            @PathVariable(name = "id") Long userId
    ) {
        return userService.getUserById(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(
            @RequestBody @Valid CreateUserRequest createUserRequest
    ) {
        return userService.createUser(createUserRequest);
    }

    @PutMapping("${end.point.id}")
    public UserDto updateUser(
            @RequestBody @Valid CreateUserRequest createUserRequest,
            @PathVariable(name = "id") Long userId
    ) {
        return userService.updateUser(userId, createUserRequest);
    }

    @DeleteMapping("${end.point.id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable(name = "id") Long userId
    ) {
        userService.deleteUser(userId);
    }

    @PostMapping("${end.point.assign.role}")
    public UserDto assignRole(
            @PathVariable(name = "id") Long userId,
            @RequestBody RoleType roleType
    ) {
        return userService.assignRole(userId, roleType);
    }
}
