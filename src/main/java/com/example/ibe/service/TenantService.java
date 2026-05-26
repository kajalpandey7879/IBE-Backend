package com.example.ibe.service;

import com.example.ibe.dto.PropertyDTO;
import com.example.ibe.dto.TenantConfigResponse;
import com.example.ibe.entity.Property;
import com.example.ibe.entity.Tenant;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.exception.TenantNotFoundException;
import com.example.ibe.repository.PropertyRepository;
import com.example.ibe.repository.TenantRepository;
import com.example.ibe.service.mapper.TenantConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Fetches tenant configuration and attaches the properties associated with the
     * requested tenant name.
     *
     * @param tenantName the tenant name to look up
     * @return the tenant configuration response with mapped property details
     */
    public TenantConfigResponse getTenantConfigWithProperties(String tenantName) {
        log.debug("Fetching config for tenant: {}", tenantName);

        Tenant tenant = fetchTenantByName(tenantName);
        TenantConfigResponse response = TenantConfigMapper.parseConfigJson(tenant.getConfigJson(), objectMapper);
        List<PropertyDTO> propertyDTOs = fetchPropertyDtosForTenant(tenant.getTenantId());

        response.setTenantId(tenant.getTenantId().toString());
        response.setProperties(propertyDTOs);
        return response;
    }

    /**
     * Fetches a tenant entity by tenant name.
     *
     * @param tenantName the tenant name to look up
     * @return the matching tenant entity
     */
    private Tenant fetchTenantByName(String tenantName) {
        try {
            return tenantRepository.findByTenantName(tenantName)
                    .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantName));
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch tenant details", ex);
        }
    }

    /**
     * Fetches and maps the properties that belong to the given tenant.
     *
     * @param tenantId the tenant identifier
     * @return the mapped property DTOs for the tenant
     */
    private List<PropertyDTO> fetchPropertyDtosForTenant(UUID tenantId) {
        List<Property> properties;
        try {
            properties = propertyRepository.findByTenant_TenantId(tenantId);
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch properties for tenant", ex);
        }

        return TenantConfigMapper.toPropertyDtos(properties);
    }
}
