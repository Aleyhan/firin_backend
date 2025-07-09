package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.request.LoginRequest;
import com.firinyonetim.backend.dto.response.LoginResponse;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.repository.UserRepository;
import com.firinyonetim.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final InMemoryUserDetailsManager inMemoryUserDetailsManager; // In-memory manager'ı inject et

    public LoginResponse login(LoginRequest request) {
        // 1. Kimlik doğrulama yap
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Token oluşturmak için UserDetails'i bul.
        // Önce in-memory'de ara, bulamazsan veritabanında ara.
        UserDetails userDetails;
        try {
            // In-memory'de kullanıcıyı bulmaya çalış
            userDetails = inMemoryUserDetailsManager.loadUserByUsername(request.getUsername());
        } catch (UsernameNotFoundException e) {
            // In-memory'de yoksa, veritabanından bul
            userDetails = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found after successful authentication."));
        }

        // 3. Token'ı oluştur
        String jwtToken = jwtTokenProvider.generateToken(userDetails);

        // 4. Response'u oluştur
        LoginResponse.LoginResponseBuilder responseBuilder = LoginResponse.builder()
                .token(jwtToken)
                .username(userDetails.getUsername());

        // Eğer kullanıcı veritabanından geldiyse (User tipindeyse), ID ve Role ekle
        if (userDetails instanceof User) {
            User dbUser = (User) userDetails;
            responseBuilder.userId(dbUser.getId());
            responseBuilder.role(dbUser.getRole());
        } else {
            // Eğer in-memory kullanıcı ise, rolü authorities'ten çıkar
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .orElseThrow()
                    .getAuthority()
                    .replace("ROLE_", "");
            responseBuilder.role(com.firinyonetim.backend.entity.Role.valueOf(role));
        }

        return responseBuilder.build();
    }
}