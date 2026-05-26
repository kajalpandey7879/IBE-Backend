package com.example.ibe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantConfigResponse {

    private String tenantId;
    private String tenantImage;
    private String tenantLogo;
    private Integer maxRooms;
    private Boolean accessibility;
    private Integer maxCapacityPerRoom;
    private Integer maxBookingRange;
    private JsonNode guests;
    private List<PropertyDTO> properties;

}