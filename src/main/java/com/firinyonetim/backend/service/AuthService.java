package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.request.LoginRequest;
import com.firinyonetim.backend.dto.response.LoginResponse;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.repository.UserRepository;
import com.firinyonetim.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        // 1. Kimlik doğrulama yap
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Kullanıcıyı veritabanından bul (Doğrulama başarılı olduğu için burada kesin bulur)
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("Kimlik doğrulama başarılı ama kullanıcı veritabanında bulunamadı."));

        // 3. Token'ı oluştur
        String jwtToken = jwtTokenProvider.generateToken(user);

        // 4. Response'u oluştur
        return LoginResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}