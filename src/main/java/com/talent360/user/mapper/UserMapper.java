package com.talent360.user.mapper;

import com.talent360.user.dto.UserResponse;
import com.talent360.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
