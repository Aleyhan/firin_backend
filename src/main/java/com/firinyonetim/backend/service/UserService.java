package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.request.UserCreateRequest;
import com.firinyonetim.backend.dto.response.UserResponse;
import com.firinyonetim.backend.entity.Role;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.UserMapper;
import com.firinyonetim.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalStateException("Kullanıcı adı zaten mevcut: " + request.getUsername());
        }
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // TCKN alanı zaten mapper tarafından set ediliyor.
        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + id));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserCreateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + id));

        if ("admin".equals(user.getUsername())) {
            throw new IllegalArgumentException("Admin kullanıcısı düzenlenemez.");
        }

        if (!user.getUsername().equals(request.getUsername()) && userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalStateException("Kullanıcı adı zaten mevcut: " + request.getUsername());
        }

        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setTckn(request.getTckn()); // YENİ ALAN GÜNCELLENDİ

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + id));

        if ("admin".equals(user.getUsername())) {
            throw new IllegalArgumentException("Admin kullanıcısı silinemez.");
        }

        userRepository.deleteById(id);
    }

    public List<UserResponse> getDrivers() {
        return userRepository.findAllByRole(Role.SOFOR).stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }
}