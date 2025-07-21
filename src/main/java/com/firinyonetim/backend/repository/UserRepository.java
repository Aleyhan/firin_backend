package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Role;
import com.firinyonetim.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // YENİ METOT: Belirli bir role sahip tüm kullanıcıları bulur (Şoförleri listelemek için)
    List<User> findAllByRole(Role role);
}