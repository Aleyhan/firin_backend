package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.request.UserCreateRequest;
import com.firinyonetim.backend.dto.response.UserResponse;
import com.firinyonetim.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // UserCreateRequest'ten User'a dönüşüm. Password'ü map'lemiyoruz, onu serviste hash'leyeceğiz.
    @Mapping(target = "password", ignore = true)
    User toUser(UserCreateRequest request);

    // User'dan UserResponse'a dönüşüm.
    UserResponse toUserResponse(User user);
}