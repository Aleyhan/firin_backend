package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.request.UserCreateRequest;
import com.firinyonetim.backend.dto.response.UserResponse;
import com.firinyonetim.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(source = "tckn", target = "tckn") // DÖNÜŞÜM İÇİN EKLENDİ
    User toUser(UserCreateRequest request);

    @Mapping(source = "tckn", target = "tckn") // DÖNÜŞÜM İÇİN EKLENDİ
    UserResponse toUserResponse(User user);
}