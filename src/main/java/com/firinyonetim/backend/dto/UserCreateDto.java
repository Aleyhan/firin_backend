package com.firinyonetim.backend.dto;
import com.firinyonetim.backend.entity.Role;
import lombok.Data;
@Data
public class UserCreateDto {
    private String name;
    private String surname;
    private String phoneNumber;
    private String password;
    private Role role;
}