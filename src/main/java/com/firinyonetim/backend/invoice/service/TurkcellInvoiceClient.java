package com.firinyonetim.backend.invoice.service;

import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceRequest;
import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceResponse;
import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TurkcellInvoiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public TurkcellInvoiceClient(@Qualifier("eWaybillRestTemplate") RestTemplate restTemplate,
                                 @Value("${turkcell.efatura.api.base-url}") String baseUrl,
                                 @Value("${turkcell.efatura.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public TurkcellInvoiceResponse createInvoice(TurkcellInvoiceRequest request) {
        String url = baseUrl + "/v1/outboxinvoice/create";
        HttpEntity<TurkcellInvoiceRequest> entity = new HttpEntity<>(request, createHeaders());
        log.info("Sending invoice to Turkcell API. LocalReferenceId: {}", request.getLocalReferenceId());
        return restTemplate.postForObject(url, entity, TurkcellInvoiceResponse.class);
    }

    public TurkcellInvoiceStatusResponse getInvoiceStatus(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxinvoice/" + turkcellApiId + "/status";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, TurkcellInvoiceStatusResponse.class).getBody();
    }

    // YENİ METOT
    public byte[] getInvoiceAsPdf(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxinvoice/" + turkcellApiId + "/pdf";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();
    }

    // YENİ METOT
    public String getInvoiceAsHtml(String turkcellApiId) {
        String url = baseUrl + "/v2/outboxinvoice/" + turkcellApiId + "/html";
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