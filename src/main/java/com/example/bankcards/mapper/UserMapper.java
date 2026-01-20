package com.example.bankcards.mapper;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User createUser(CreateUserRequest createUserRequest);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToString")
    UserDto toDto(User user);

    @Named("rolesToString")
    default Set<String> rolesToString(Set<Role> roles) {
        if (roles == null) {
            return new HashSet<>();
        }

        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}

