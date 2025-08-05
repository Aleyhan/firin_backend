package com.firinyonetim.backend.ewaybill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling // Zamanlanmış görevleri aktif etmek için
public class EWaybillConfig {

    @Bean("eWaybillRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}