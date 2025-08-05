package com.firinyonetim.backend.dto.response;
import com.firinyonetim.backend.entity.Role;
import lombok.Data;
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String surname;
    private String phoneNumber;
    private String tckn; // YENİ ALAN
    private Role role;
}