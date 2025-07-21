package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.request.UserCreateRequest;
import com.firinyonetim.backend.dto.response.UserResponse;
import com.firinyonetim.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    // YENİ ENDPOINT: Sadece şoför rolündeki kullanıcıları getirir
    @GetMapping("/drivers")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<List<UserResponse>> getDrivers() {
        return ResponseEntity.ok(userService.getDrivers());
    }
}