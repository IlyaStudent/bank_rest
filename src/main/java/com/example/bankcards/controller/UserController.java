package com.example.bankcards.controller;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "User management (admin only)")
@RestController
@RequiredArgsConstructor
@RequestMapping("${end.point.users}")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users", description = "Returns paginated list of all users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    @GetMapping
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @Operation(summary = "Get user by ID", description = "Returns user details by user ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    @GetMapping("${end.point.id}")
    public UserDto getUser(
            @Parameter(description = "User ID") @PathVariable(name = "id") Long userId
    ) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "Create new user", description = "Creates a new user with specified roles")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "422", description = "Invalid role specified"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(
            @RequestBody @Valid CreateUserRequest createUserRequest
    ) {
        return userService.createUser(createUserRequest);
    }

    @Operation(summary = "Update user", description = "Updates user information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    @PutMapping("${end.point.id}")
    public UserDto updateUser(
            @RequestBody @Valid CreateUserRequest createUserRequest,
            @Parameter(description = "User ID") @PathVariable(name = "id") Long userId
    ) {
        return userService.updateUser(userId, createUserRequest);
    }

    @Operation(summary = "Delete user", description = "Deletes a user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    @DeleteMapping("${end.point.id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @Parameter(description = "User ID") @PathVariable(name = "id") Long userId
    ) {
        userService.deleteUser(userId);
    }

    @Operation(summary = "Assign role to user", description = "Assigns a new role to the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    @PostMapping("${end.point.assign.role}")
    public UserDto assignRole(
            @Parameter(description = "User ID") @PathVariable(name = "id") Long userId,
            @RequestBody RoleType roleType
    ) {
        return userService.assignRole(userId, roleType);
    }
}
