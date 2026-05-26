package com.example.ibe.service.mapper;

import com.example.ibe.dto.PropertyDTO;
import com.example.ibe.dto.TenantConfigResponse;
import com.example.ibe.entity.Property;
import com.example.ibe.exception.InvalidTenantConfigException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public final class TenantConfigMapper {

    private TenantConfigMapper() {
    }

    public static TenantConfigResponse parseConfigJson(String configJson, ObjectMapper objectMapper) {
        if (configJson == null || configJson.isBlank()) {
            throw new InvalidTenantConfigException("Tenant config JSON is missing", null);
        }

        try {
            return objectMapper.readValue(configJson, TenantConfigResponse.class);
        } catch (Exception ex) {
            log.error("Failed to parse config JSON", ex);
            throw new InvalidTenantConfigException("Invalid config JSON format", ex);
        }
    }

    public static List<PropertyDTO> toPropertyDtos(List<Property> properties) {
        return properties.stream()
                .map(property -> PropertyDTO.builder()
                        .propertyId(property.getPropertyId())
                        .propertyName(property.getPropertyName())
                        .build())
                .toList();
    }
}