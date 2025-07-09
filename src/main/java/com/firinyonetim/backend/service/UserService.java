package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.request.UserCreateRequest;
import com.firinyonetim.backend.dto.response.UserResponse;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.mapper.UserMapper;
import com.firinyonetim.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse createUser(UserCreateRequest request) {
        // DTO'dan Entity'e dönüşüm
        User user = userMapper.toUser(request);
        // Password'ü hash'le ve set et
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Kaydet
        User savedUser = userRepository.save(user);
        // Kaydedilen Entity'i Response DTO'suna dönüştür ve geri döndür
        return userMapper.toUserResponse(savedUser);
    }
}