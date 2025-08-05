package com.firinyonetim.backend.config;

import com.firinyonetim.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health").permitAll()

                        // DÜZELTME BURADA: Path pattern'i doğru formata getirildi.
                        // Bu kural, /api/ewaybills/{bir-uuid-degeri}/pdf şeklindeki tüm GET isteklerine izin verir.
                        .requestMatchers(HttpMethod.GET, "/api/ewaybills/{id}/pdf").authenticated()

                        // ŞOFÖR İÇİN ÖZEL KURALLAR
                        .requestMatchers("/api/driver/**").hasRole("SOFOR")
                        .requestMatchers(HttpMethod.GET, "/api/customers").hasAnyRole("YONETICI", "DEVELOPER", "MUHASEBE", "SOFOR")
                        .requestMatchers(HttpMethod.GET, "/api/routes").hasAnyRole("YONETICI", "DEVELOPER", "MUHASEBE", "SOFOR")
                        .requestMatchers(HttpMethod.GET, "/api/products").hasAnyRole("YONETICI", "DEVELOPER", "MUHASEBE", "SOFOR")
                        .requestMatchers(HttpMethod.GET, "/api/routes/{id}").hasAnyRole("YONETICI", "DEVELOPER", "MUHASEBE", "SOFOR")
                        .requestMatchers(HttpMethod.GET, "/api/customers/{id}/products").hasAnyRole("YONETICI", "DEVELOPER", "SOFOR", "MUHASEBE")

                        // YÖNETİCİ/MUHASEBE İÇİN GENEL KURALLAR
                        .requestMatchers("/api/admin/**").hasAnyRole("YONETICI", "DEVELOPER", "MUHASEBE")
                        .anyRequest().hasAnyRole("YONETICI", "DEVELOPER", "MUHASEBE")
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5176", "http://19-1.107:5178", "http://192.168.1.107:5173", "http://192.168.1.45:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}