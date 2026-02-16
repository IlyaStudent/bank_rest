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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private UserDto userDto;
    private User user;
    private List<User> userList;
    private Page<User> userPage;
    private Long userId;
    private String email;
    private String username;
    private String password;
    private String encodedPassword;
    private Role role;
    private RoleType roleType;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "test";
        email = "test@example.com";
        password = "password";
        encodedPassword = "encodedPassword";
        roleType = RoleType.USER;
        role = new Role(1L, roleType);

        pageable = PageRequest.of(0, 10);

        createUserRequest = CreateUserRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .roles(new HashSet<>())
                .build();

        updateUserRequest = UpdateUserRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .roles(new HashSet<>())
                .build();

        user = User.builder()
                .id(userId)
                .username(username)
                .email(email)
                .password(password)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        userDto = UserDto.builder()
                .id(userId)
                .username(username)
                .email(email)
                .roles(new HashSet<>(Set.of(roleType.name())))
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("test2")
                .email("test2@example.com")
                .password(password)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        userList = List.of(user, user2);

        userPage = new PageImpl<>(userList, pageable, userList.size());
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {
        @Test
        @DisplayName("Should create user successfully with default role when roles not specified")
        void shouldCreateUserSuccessfullyWithDefaultRole() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userMapper.createUser(createUserRequest)).thenReturn(user);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(role));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = userService.createUser(createUserRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getRoles()).contains(RoleType.USER.name());

            verify(userRepository).existsByUsername(username);
            verify(userRepository).existsByEmail(email);
            verify(userMapper).createUser(createUserRequest);
            verify(passwordEncoder).encode(password);
            verify(roleRepository).findByName(RoleType.USER);
            verify(userRepository).save(user);
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("Should create user successfully with specified roles")
        void shouldCreateUserSuccessfullyWithSpecifiedRoles() {
            Role adminRole = new Role(2L, RoleType.ADMIN);

            CreateUserRequest adminRequest = CreateUserRequest.builder()
                    .username(username)
                    .email(email)
                    .password(password)
                    .roles(Set.of(RoleType.ADMIN.name()))
                    .build();

            UserDto adminUserDto = UserDto.builder()
                    .id(userId)
                    .username(username)
                    .email(email)
                    .roles(Set.of(RoleType.ADMIN.name()))
                    .build();

            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userMapper.createUser(adminRequest)).thenReturn(user);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(adminUserDto);

            UserDto result = userService.createUser(adminRequest);

            assertThat(result).isNotNull();
            assertThat(result.getRoles()).contains(RoleType.ADMIN.name());

            verify(roleRepository).findByName(RoleType.ADMIN);
            verify(roleRepository, never()).findByName(RoleType.USER);
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userMapper.createUser(createUserRequest)).thenReturn(user);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(role));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.createUser(createUserRequest);

            verify(passwordEncoder).encode(password);
            assertThat(user.getPassword()).isEqualTo(encodedPassword);
        }

        @Test
        @DisplayName("Should throw ResourceExistsException when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            when(userRepository.existsByUsername(username)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(createUserRequest))
                    .isInstanceOf(ResourceExistsException.class)
                    .hasMessageContaining(username);

            verify(userRepository).existsByUsername(username);
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ResourceExistsException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(createUserRequest))
                    .isInstanceOf(ResourceExistsException.class)
                    .hasMessageContaining(email);

            verify(userRepository).existsByUsername(username);
            verify(userRepository).existsByEmail(email);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when default role not found in database")
        void shouldThrowExceptionWhenDefaultRoleNotFound() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userMapper.createUser(createUserRequest)).thenReturn(user);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(createUserRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(roleRepository).findByName(RoleType.USER);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when specified role name is invalid")
        void shouldThrowExceptionWhenRoleNameInvalid() {
            createUserRequest.setRoles(Set.of("INVALID_ROLE"));

            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userMapper.createUser(createUserRequest)).thenReturn(user);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

            assertThatThrownBy(() -> userService.createUser(createUserRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("INVALID_ROLE");

            verify(userRepository, never()).save(any(User.class));
        }

    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = userService.getUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getRoles()).contains(roleType.name());

            verify(userRepository).findById(userId);
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(userId.toString());

            verify(userRepository).findById(userId);
            verify(userMapper, never()).toDto(user);
        }

    }

    @Nested
    @DisplayName("getUserByUsername")
    class GetUserByUsername {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = userService.getUserByUsername(username);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getRoles()).contains(roleType.name());

            verify(userRepository).findByUsername(username);
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername(username))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(username);

            verify(userRepository).findByUsername(username);
            verify(userMapper, never()).toDto(user);
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("Should return page with users")
        void shouldReturnPageWithUsers() {
            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toDto(any(User.class))).thenReturn(userDto);

            Page<UserDto> result = userService.getAllUsers(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getNumber()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getContent().getFirst().getUsername()).isEqualTo(username);

            verify(userRepository).findAll(pageable);
            verify(userMapper, times(2)).toDto(any(User.class));
        }

        @Test
        @DisplayName("Should return empty page with no users")
        void shouldReturnEmptyPageWithNoUsers() {
            userPage = Page.empty(pageable);

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<UserDto> result = userService.getAllUsers(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.hasContent()).isFalse();

            verify(userRepository).findAll(pageable);
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("Should return correct page metadata")
        void shouldReturnCorrectPageMetadata() {
            pageable = PageRequest.of(1, 5);
            userList = List.of(
                    User.builder()
                            .id(6L)
                            .username("user6")
                            .build()
            );
            userPage = new PageImpl<>(userList, pageable, 11);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toDto(any(User.class)))
                    .thenReturn(new UserDto(
                            6L,
                            "user6",
                            "test@test.com",
                            Set.of())
                    );

            Page<UserDto> result = userService.getAllUsers(pageable);

            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(11);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.hasPrevious()).isTrue();
            assertThat(result.isFirst()).isFalse();
            assertThat(result.isLast()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("Should update user successfully with new username")
        void shouldUpdateUserSuccessfullyWithNewUsername() {
            String newUsername = "newUsername";
            updateUserRequest.setUsername(newUsername);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            UserDto expectedDto = UserDto.builder()
                    .id(userId)
                    .username(newUsername)
                    .email(email)
                    .roles(Set.of(roleType.name()))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername(newUsername)).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(expectedDto);

            UserDto result = userService.updateUser(userId, updateUserRequest);

            assertThat(result.getUsername()).isEqualTo(newUsername);
            assertThat(user.getUsername()).isEqualTo(newUsername);
            verify(userRepository).existsByUsername(newUsername);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should update user without changing username when null")
        void shouldUpdateUserSuccessfullyWithoutChangingUsernameWhenNull() {
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = userService.updateUser(userId, updateUserRequest);

            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(user.getUsername()).isEqualTo(username);
            verify(userRepository, never()).existsByUsername(anyString());
        }

        @Test
        @DisplayName("Should update user without changing username when blank")
        void shouldUpdateUserSuccessfullyWithoutChangingUsernameWhenBlank() {
            updateUserRequest.setUsername("   ");
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = userService.updateUser(userId, updateUserRequest);

            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(user.getUsername()).isEqualTo(username);
            verify(userRepository, never()).existsByUsername(anyString());
        }

        @Test
        @DisplayName("Should not check uniqueness when username unchanged")
        void shouldNotCheckUniquenessWhenUsernameUnchanged() {
            updateUserRequest.setUsername(username);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            verify(userRepository, never()).existsByUsername(anyString());
        }

        @Test
        @DisplayName("Should not check uniqueness when email unchanged")
        void shouldNotCheckUniquenessWhenEmailUnchanged() {
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(email);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should update user successfully with new email")
        void shouldUpdateUserSuccessfullyWithNewEmail() {
            String newEmail = "newemail@example.com";
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(newEmail);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            UserDto expectedDto = UserDto.builder()
                    .id(userId)
                    .username(username)
                    .email(newEmail)
                    .roles(Set.of(roleType.name()))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(newEmail)).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(expectedDto);

            UserDto result = userService.updateUser(userId, updateUserRequest);

            assertThat(result.getEmail()).isEqualTo(newEmail);
            assertThat(user.getEmail()).isEqualTo(newEmail);
            verify(userRepository).existsByEmail(newEmail);
        }

        @Test
        @DisplayName("Should update user successfully without changing email when null")
        void shouldUpdateUserSuccessfullyWithoutChangingEmailWhenNull() {
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            assertThat(user.getEmail()).isEqualTo(email);
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should update user successfully without changing email when blank")
        void shouldUpdateUserSuccessfullyWithoutChangingEmailWhenBlank() {
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail("   ");
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            assertThat(user.getEmail()).isEqualTo(email);
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should update user successfully with new password")
        void shouldUpdateUserSuccessfullyWithNewPassword() {
            String newPassword = "newPassword";
            String newEncodedPassword = "encodedNewPassword";
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(newPassword);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            assertThat(user.getPassword()).isEqualTo(newEncodedPassword);
            verify(passwordEncoder).encode(newPassword);
        }

        @Test
        @DisplayName("Should update user successfully without changing password when null")
        void shouldUpdateUserSuccessfullyWithoutChangingPasswordWhenNull() {
            String originalPassword = user.getPassword();
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            assertThat(user.getPassword()).isEqualTo(originalPassword);
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should update user successfully without changing password when blank")
        void shouldUpdateUserSuccessfullyWithoutChangingPasswordWhenBlank() {
            String originalPassword = user.getPassword();
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword("   ");
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            assertThat(user.getPassword()).isEqualTo(originalPassword);
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should update user successfully with new roles")
        void shouldUpdateUserSuccessfullyWithNewRoles() {
            Role adminRole = new Role(2L, RoleType.ADMIN);
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(Set.of(RoleType.ADMIN.name()));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(RoleType.ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            verify(roleRepository).findByName(RoleType.ADMIN);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should update user successfully without changing roles when null")
        void shouldUpdateUserSuccessfullyWithoutChangingRolesWhenNull() {
            Set<Role> originalRoles = user.getRoles();
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            assertThat(user.getRoles()).isEqualTo(originalRoles);
            verify(roleRepository, never()).findByName(any(RoleType.class));
        }

        @Test
        @DisplayName("Should update user successfully without changing roles when empty")
        void shouldUpdateUserSuccessfullyWithoutChangingRolesWhenEmpty() {
            Set<Role> originalRoles = user.getRoles();
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(new HashSet<>());

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            userService.updateUser(userId, updateUserRequest);

            assertThat(user.getRoles()).isEqualTo(originalRoles);
            verify(roleRepository, never()).findByName(any(RoleType.class));
        }

        @Test
        @DisplayName("Should throw exception when role not found")
        void shouldThrowExceptionWhenRoleNotFound() {
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(Set.of(RoleType.ADMIN.name()));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(RoleType.ADMIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when role name not valid")
        void shouldThrowExceptionWhenRoleNameNotValid() {
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(Set.of("INVALID_ROLE"));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("INVALID_ROLE");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when username already taken")
        void shouldThrowExceptionWhenUsernameAlreadyTaken() {
            String newUsername = "takenUsername";
            updateUserRequest.setUsername(newUsername);
            updateUserRequest.setEmail(null);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername(newUsername)).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequest))
                    .isInstanceOf(ResourceExistsException.class)
                    .hasMessageContaining(newUsername);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already taken")
        void shouldThrowExceptionWhenEmailAlreadyTaken() {
            String newEmail = "taken@example.com";
            updateUserRequest.setUsername(null);
            updateUserRequest.setEmail(newEmail);
            updateUserRequest.setPassword(null);
            updateUserRequest.setRoles(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(newEmail)).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequest))
                    .isInstanceOf(ResourceExistsException.class)
                    .hasMessageContaining(newEmail);

            verify(userRepository, never()).save(any(User.class));
        }

    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            when(userRepository.existsById(userId)).thenReturn(true);

            userService.deleteUser(userId);

            verify(userRepository).existsById(userId);
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(userId.toString());

            verify(userRepository).existsById(userId);
        }
    }

    @Nested
    @DisplayName("assignRole")
    class AssignRole {

        @Test
        @DisplayName("Should assign role to user")
        void shouldAssignRoleToUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(roleType)).thenReturn(Optional.of(role));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = userService.assignRole(userId, roleType);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getRoles()).contains(RoleType.USER.name());

            verify(userRepository).findById(userId);
            verify(roleRepository).findByName(roleType);
            verify(userRepository).save(user);
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("Should add new role to existing roles")
        void shouldAddNewRoleToExistingRoles() {
            Role adminRole = new Role(2L, RoleType.ADMIN);
            Set<Role> mutableRoles = new HashSet<>(Set.of(role));
            user.setRoles(mutableRoles);

            UserDto expectedDto = UserDto.builder()
                    .id(userId)
                    .username(username)
                    .email(email)
                    .roles(Set.of(RoleType.USER.name(), RoleType.ADMIN.name()))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(RoleType.ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(expectedDto);

            UserDto result = userService.assignRole(userId, RoleType.ADMIN);

            assertThat(user.getRoles()).hasSize(2);
            assertThat(user.getRoles()).contains(role, adminRole);
            assertThat(result.getRoles()).contains(RoleType.USER.name(), RoleType.ADMIN.name());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.assignRole(userId, roleType))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(userId.toString());

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Should throw exception when role not found")
        void shouldThrowExceptionWhenRoleNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(roleType)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.assignRole(userId, roleType))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(roleType.toString());

            verify(userRepository).findById(userId);
            verify(roleRepository).findByName(roleType);
        }

    }
}
