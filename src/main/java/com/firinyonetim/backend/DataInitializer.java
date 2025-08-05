package com.firinyonetim.backend;

import com.firinyonetim.backend.entity.Role;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void run(String... args) throws Exception {
        printAllIPAddresses();

        String ip = environment.getProperty("server.address", "localhost");
        String port = environment.getProperty("server.port", "8080");
        System.out.println("Application running on IP: " + ip + ", Port: " + port);

        // Check if "admin" user exists at startup
        if (userRepository.findByUsername("admin").isEmpty()) {
            System.out.println("Admin user not found, creating...");

            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setName("Admin");
            adminUser.setSurname("Admin");
            adminUser.setRole(Role.YONETICI);
            adminUser.setPhoneNumber("0000000000");

            userRepository.save(adminUser);
            System.out.println("Admin user created successfully.");
        } else {
            System.out.println("Admin user already exists.");
        }
    }

    private static void printAllIPAddresses() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaces)) {
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        System.out.println("Detected IP: " + addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error listing IP addresses: " + e.getMessage());
        }
    }
}