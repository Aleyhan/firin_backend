package com.firinyonetim.backend.ewaybill.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.ewaybill.dto.turkcell.GibUser;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiRequest;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiResponse;
import lombok.extern.slf4j.Slf4j; // YENİ IMPORT
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException; // YENİ IMPORT
import java.nio.file.Files; // YENİ IMPORT
import java.nio.file.Paths; // YENİ IMPORT
import java.nio.file.StandardOpenOption; // YENİ IMPORT

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipInputStream;

@Slf4j // YENİ ANOTASYON
@Service
public class TurkcellEWaybillClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public TurkcellEWaybillClient(@Qualifier("eWaybillRestTemplate") RestTemplate restTemplate,
                                  @Value("${turkcell.ewaybill.api.base-url}") String baseUrl,
                                  @Value("${turkcell.ewaybill.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public TurkcellApiResponse createEWaybill(TurkcellApiRequest request) {
        String url = baseUrl + "/v1/outboxdespatch/create";
        HttpEntity<TurkcellApiRequest> entity = new HttpEntity<>(request, createHeaders());
        return restTemplate.postForObject(url, entity, TurkcellApiResponse.class);
    }

    public TurkcellApiResponse getEWaybillStatus(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxdespatch/" + turkcellApiId + "/status";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, TurkcellApiResponse.class).getBody();
    }

    public byte[] getEWaybillAsPdf(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxdespatch/" + turkcellApiId + "/pdf";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();
    }

    public String getEWaybillAsHtml(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxdespatch/" + turkcellApiId + "/html";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
    }

    public List<GibUser> getGibUserList() {
        String url = baseUrl + "/v1/gibuser/recipient/zip";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        byte[] zipBytes = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();

        if (zipBytes == null) {
            return Collections.emptyList();
        }

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            if (zis.getNextEntry() != null) {
                String jsonContent = new String(zis.readAllBytes(), StandardCharsets.UTF_8);

                if (jsonContent.startsWith("\uFEFF")) {
                    jsonContent = jsonContent.substring(1);
                }

                // YENİ: JSON içeriğini dosyaya yaz
                try {
                    Files.writeString(Paths.get("gib_user_list.txt"), jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    log.info("Successfully wrote GIB user list to gib_user_list.txt");
                } catch (IOException e) {
                    log.error("Failed to write GIB user list to file.", e);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(jsonContent, new TypeReference<List<GibUser>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse GIB user list from zip file.", e);
            throw new RuntimeException("Could not retrieve or parse GIB user list.", e);
        }
        return Collections.emptyList();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        return headers;
    }
}