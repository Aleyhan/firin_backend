package com.firinyonetim.backend;

import com.firinyonetim.backend.entity.Role;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Uygulama başladığında, "admin" kullanıcısı var mı diye kontrol et
        if (userRepository.findByUsername("admin").isEmpty()) {
            System.out.println("Admin kullanıcısı bulunamadı, oluşturuluyor...");

            User adminUser = new User();
            adminUser.setUsername("admin");
            // Şifreyi mutlaka hash'leyerek kaydet
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setName("Admin");
            adminUser.setSurname("Admin");
            adminUser.setRole(Role.YONETICI);
            adminUser.setPhoneNumber("0000000000");

            userRepository.save(adminUser);
            System.out.println("Admin kullanıcısı başarıyla oluşturuldu.");
        } else {
            System.out.println("Admin kullanıcısı zaten mevcut.");
        }
    }
}