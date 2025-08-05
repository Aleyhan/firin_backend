package com.firinyonetim.backend.ewaybill.service;

import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiRequest;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    // DÜZELTME: Endpoint versiyonu v2 olarak değiştirildi.
    public byte[] getEWaybillAsPdf(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxdespatch/" + turkcellApiId + "/pdf";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();
    }

    // DÜZELTME: Endpoint versiyonu v2 olarak değiştirildi.
    public String getEWaybillAsHtml(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxdespatch/" + turkcellApiId + "/html";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        return headers;
    }
}