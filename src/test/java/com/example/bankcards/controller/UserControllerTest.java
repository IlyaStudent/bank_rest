package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.exception.ResourceExistsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDto userDto;
    private UserDto updatedUserDto;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private Long userId;
    private String username;
    private String email;
    private String updatedUsername;
    private String updatedEmail;
    private Page<UserDto> userDtoPage;

    private static final String USERS_URL = "/api/users";
    private static final String USER_BY_ID_URL = "/api/users/{id}";
    private static final String ASSIGN_ROLE_URL = "/api/users/{id}/roles";

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "test";
        email = "test@example.com";
        updatedUsername = "newUsername";
        updatedEmail = "newEmail@example.com";

        userDto = UserDto.builder()
                .id(userId)
                .username(username)
                .email(email)
                .roles(new HashSet<>())
                .build();

        updatedUserDto = UserDto.builder()
                .id(userId)
                .username(updatedUsername)
                .email(updatedEmail)
                .roles(new HashSet<>())
                .build();

        createUserRequest = CreateUserRequest.builder()
                .username(username)
                .email(email)
                .password("password")
                .roles(new HashSet<>())
                .build();

        updateUserRequest = UpdateUserRequest.builder()
                .username(updatedUsername)
                .email(updatedEmail)
                .password("password")
                .roles(new HashSet<>())
                .build();

        userDtoPage = new PageImpl<>(
                List.of(userDto),
                PageRequest.of(0, 10),
                1
        );
    }

    @Nested
    @DisplayName("GET " + USERS_URL)
    class GetAllUsers {

        @Test
        @DisplayName("Should return page of users")
        @WithMockUser(roles = "ADMIN")
        void shouldReturnPageOfUsers() throws Exception {
            when(userService.getAllUsers(any(Pageable.class))).thenReturn(userDtoPage);

            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].username").value(username))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(userService).getAllUsers(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no users")
        @WithMockUser(roles = "ADMIN")
        void shouldReturnEmptyPageWhenNoUsers() throws Exception {
            Page<UserDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(userService.getAllUsers(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(userService).getAllUsers(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).getAllUsers(any());
        }

        @Test
        @DisplayName("Should return 403 when not admin")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isForbidden());

            verify(userService, never()).getAllUsers(any());
        }
    }

    @Nested
    @DisplayName("GET " + USER_BY_ID_URL)
    class GetUserById {
        @Test
        @DisplayName("Should return user when found")
        @WithMockUser(roles = "ADMIN")
        void shouldReturnUserWhenFound() throws Exception {
            when(userService.getUserById(userId)).thenReturn(userDto);

            mockMvc.perform(get(USER_BY_ID_URL, userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.email").value(email));

            verify(userService).getUserById(userId);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(USER_BY_ID_URL, userId))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).getUserById(any());
        }

        @Test
        @DisplayName("Should return 403 when not admin")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(get(USER_BY_ID_URL, userId))
                    .andExpect(status().isForbidden());

            verify(userService, never()).getUserById(any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userService.getUserById(userId)).thenThrow(
                    ResourceNotFoundException.user(userId)
            );

            mockMvc.perform(get(USER_BY_ID_URL, userId))
                    .andExpect(status().isNotFound());

            verify(userService).getUserById(userId);
        }
    }

    @Nested
    @DisplayName("POST " + USERS_URL)
    class CreateUser {

        @Test
        @DisplayName("Should create user successfully")
        @WithMockUser(roles = "ADMIN")
        void shouldCreateUserSuccessfully() throws Exception {
            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userDto);

            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createUserRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value(username));

            verify(userService).createUser(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenValidationFails() throws Exception {
            CreateUserRequest invalidRequest = CreateUserRequest.builder()
                    .username("")
                    .email("")
                    .password("password")
                    .build();

            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).createUser(any());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post(USERS_URL))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).createUser(any());
        }

        @Test
        @DisplayName("Should return 403 when not admin")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(post(USERS_URL))
                    .andExpect(status().isForbidden());

            verify(userService, never()).createUser(any());
        }

        @Test
        @DisplayName("Should return 409 when username exists")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn409WhenUsernameExists() throws Exception {
            when(userService.createUser(any(CreateUserRequest.class))).thenThrow(
                    ResourceExistsException.username(username)
            );

            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createUserRequest)))
                    .andExpect(status().isConflict());

            verify(userService).createUser(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 409 when email exists")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn409WhenEmailExists() throws Exception {
            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenThrow(ResourceExistsException.email(email));

            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createUserRequest)))
                    .andExpect(status().isConflict());

            verify(userService).createUser(any(CreateUserRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT " + USER_BY_ID_URL)
    class UpdateUser {

        @Test
        @DisplayName("Should update user successfully")
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateUserSuccessfully() throws Exception {
            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(updatedUserDto);

            mockMvc.perform(put(USER_BY_ID_URL, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(updatedUsername))
                    .andExpect(jsonPath("$.email").value(updatedEmail));

            verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenValidationFails() throws Exception {
            UpdateUserRequest invalidRequest = UpdateUserRequest.builder()
                    .username("")
                    .email("invalid-email")
                    .build();

            mockMvc.perform(put(USER_BY_ID_URL, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(put(USER_BY_ID_URL, userId))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @DisplayName("Should return 403 when not admin")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(put(USER_BY_ID_URL, userId))
                    .andExpect(status().isForbidden());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                    .thenThrow(ResourceNotFoundException.user(userId));

            mockMvc.perform(put(USER_BY_ID_URL, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andExpect(status().isNotFound());

            verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("Should return 409 when email exists")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn409WhenEmailExists() throws Exception {
            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                    .thenThrow(ResourceExistsException.email(email));

            mockMvc.perform(put(USER_BY_ID_URL, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andExpect(status().isConflict());

            verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE " + USER_BY_ID_URL)
    class DeleteUser {

        @Test
        @DisplayName("Should delete user successfully")
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteUserSuccessfully() throws Exception {
            doNothing().when(userService).deleteUser(userId);

            mockMvc.perform(delete(USER_BY_ID_URL, userId))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(userId);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(delete(USER_BY_ID_URL, userId))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).deleteUser(any());
        }

        @Test
        @DisplayName("Should return 403 when not admin")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(delete(USER_BY_ID_URL, userId))
                    .andExpect(status().isForbidden());

            verify(userService, never()).deleteUser(any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenUserNotFound() throws Exception {
            doThrow(ResourceNotFoundException.user(userId))
                    .when(userService).deleteUser(userId);

            mockMvc.perform(delete(USER_BY_ID_URL, userId))
                    .andExpect(status().isNotFound());

            verify(userService).deleteUser(userId);
        }
    }

    @Nested
    @DisplayName("POST " + ASSIGN_ROLE_URL)
    class AssignRole {

        @Test
        @DisplayName("Should assign role successfully")
        @WithMockUser(roles = "ADMIN")
        void shouldAssignRoleSuccessfully() throws Exception {
            UserDto responseDto = UserDto.builder()
                    .id(userId)
                    .username(updatedUsername)
                    .email(updatedEmail)
                    .roles(Set.of("ADMIN"))
                    .build();
            when(userService.assignRole(eq(userId), any(RoleType.class))).thenReturn(responseDto);

            mockMvc.perform(post(ASSIGN_ROLE_URL, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(RoleType.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles").isArray())
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0]").value("ADMIN"));

            verify(userService).assignRole(userId, RoleType.ADMIN);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post(ASSIGN_ROLE_URL, userId))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).assignRole(any(), any());
        }

        @Test
        @DisplayName("Should return 403 when not admin")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(post(ASSIGN_ROLE_URL, userId))
                    .andExpect(status().isForbidden());

            verify(userService, never()).assignRole(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userService.assignRole(eq(userId), any(RoleType.class)))
                    .thenThrow(ResourceNotFoundException.user(userId));

            mockMvc.perform(post(ASSIGN_ROLE_URL, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(RoleType.ADMIN)))
                    .andExpect(status().isNotFound());

            verify(userService).assignRole(userId, RoleType.ADMIN);
        }
    }
}
