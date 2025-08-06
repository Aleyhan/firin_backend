package com.firinyonetim.backend.ewaybill.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GibUser {
    // DÜZELTME: @JsonProperty içindeki değerler PascalCase olarak güncellendi.
    @JsonProperty("Identifier")
    private String identifier;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Alias")
    private String alias;

    @JsonProperty("AppType")
    private int appType;
}