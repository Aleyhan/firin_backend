package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.request.UserCreateRequest;
import com.firinyonetim.backend.dto.response.UserResponse;
import com.firinyonetim.backend.entity.Role;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.mapper.UserMapper;
import com.firinyonetim.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse createUser(UserCreateRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    // YENİ METOT
    public List<UserResponse> getDrivers() {
        return userRepository.findAllByRole(Role.SOFOR).stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }
}