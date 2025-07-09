package com.firinyonetim.backend.dto.response;
import com.firinyonetim.backend.entity.Role;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private Role role;
}