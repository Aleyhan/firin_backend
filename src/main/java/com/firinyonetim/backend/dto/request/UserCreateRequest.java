package com.firinyonetim.backend.dto.request;
import com.firinyonetim.backend.entity.Role;
import lombok.Data;
@Data
public class UserCreateRequest {
    private String username;
    private String password;
    private String name;
    private String surname;
    private String phoneNumber;
    private String tckn; // YENİ ALAN
    private Role role;
}