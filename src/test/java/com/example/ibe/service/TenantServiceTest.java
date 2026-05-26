package com.example.ibe.service;

import com.example.ibe.dto.TenantConfigResponse;
import com.example.ibe.entity.Property;
import com.example.ibe.entity.Tenant;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.exception.InvalidTenantConfigException;
import com.example.ibe.exception.TenantNotFoundException;
import com.example.ibe.repository.PropertyRepository;
import com.example.ibe.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TenantService tenantService;

    @Test
    void getTenantConfigWithPropertiesReturnsMappedResponse() {
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setTenantName("radisson");
        tenant.setConfigJson(
                "{\"tenantImage\":\"img\",\"tenantLogo\":\"logo\",\"maxRooms\":5,\"accessibility\":true,\"maxCapacityPerRoom\":4,\"maxBookingRange\":30,\"guests\":{\"adults\":\"18+\"}}");

        Property property = new Property();
        property.setPropertyId(UUID.randomUUID());
        property.setPropertyName("Ocean View Resort");

        when(tenantRepository.findByTenantName("radisson")).thenReturn(Optional.of(tenant));
        when(propertyRepository.findByTenant_TenantId(tenantId)).thenReturn(List.of(property));

        TenantConfigResponse response = tenantService.getTenantConfigWithProperties("radisson");

        assertEquals(tenantId.toString(), response.getTenantId());
        assertEquals("img", response.getTenantImage());
        assertEquals("logo", response.getTenantLogo());
        assertEquals(5, response.getMaxRooms());
        assertEquals(true, response.getAccessibility());
        assertEquals(4, response.getMaxCapacityPerRoom());
        assertEquals(30, response.getMaxBookingRange());
        assertEquals(1, response.getProperties().size());
        assertEquals("Ocean View Resort", response.getProperties().get(0).getPropertyName());
        assertEquals("18+", response.getGuests().get("adults").asText());
    }

    @Test
    void getTenantConfigWithPropertiesThrowsWhenTenantMissing() {
        when(tenantRepository.findByTenantName("missing")).thenReturn(Optional.empty());

        TenantNotFoundException ex = assertThrows(
                TenantNotFoundException.class,
                () -> tenantService.getTenantConfigWithProperties("missing"));

        assertTrue(ex.getMessage().contains("Tenant not found"));
    }

    @Test
    void getTenantConfigWithPropertiesThrowsWhenConfigJsonInvalid() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(UUID.randomUUID());
        tenant.setTenantName("radisson");
        tenant.setConfigJson("{bad json}");

        when(tenantRepository.findByTenantName("radisson")).thenReturn(Optional.of(tenant));

        InvalidTenantConfigException ex = assertThrows(
                InvalidTenantConfigException.class,
                () -> tenantService.getTenantConfigWithProperties("radisson"));

        assertTrue(ex.getMessage().contains("Invalid config JSON format"));
    }

    @Test
    void getTenantConfigWithPropertiesThrowsWhenPropertyFetchFails() {
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setTenantName("radisson");
        tenant.setConfigJson("{}");

        when(tenantRepository.findByTenantName("radisson")).thenReturn(Optional.of(tenant));
        when(propertyRepository.findByTenant_TenantId(tenantId))
                .thenThrow(new DataRetrievalFailureException("db error"));

        DataProcessingException ex = assertThrows(
                DataProcessingException.class,
                () -> tenantService.getTenantConfigWithProperties("radisson"));

        assertTrue(ex.getMessage().contains("Failed to fetch properties for tenant"));
    }

    @Test
    void getTenantConfigWithPropertiesThrowsWhenConfigJsonIsNull() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(UUID.randomUUID());
        tenant.setTenantName("radisson");
        tenant.setConfigJson(null);

        when(tenantRepository.findByTenantName("radisson")).thenReturn(Optional.of(tenant));

        InvalidTenantConfigException ex = assertThrows(
                InvalidTenantConfigException.class,
                () -> tenantService.getTenantConfigWithProperties("radisson"));

        assertTrue(ex.getMessage().contains("Tenant config JSON is missing"));
    }

    @Test
    void getTenantConfigWithPropertiesMapsStringAndInvalidNumericValues() {
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setTenantName("radisson");
        tenant.setConfigJson(
                "{\"tenantLogo\":123,\"maxRooms\":\"7\",\"accessibility\":\"true\",\"maxCapacityPerRoom\":null,\"maxBookingRange\":\"60\",\"guests\":null}");

        when(tenantRepository.findByTenantName("radisson")).thenReturn(Optional.of(tenant));
        when(propertyRepository.findByTenant_TenantId(tenantId)).thenReturn(List.of());

        TenantConfigResponse response = tenantService.getTenantConfigWithProperties("radisson");

        assertNull(response.getTenantImage());
        assertEquals("123", response.getTenantLogo());
        assertEquals(7, response.getMaxRooms());
        assertEquals(true, response.getAccessibility());
        assertNull(response.getMaxCapacityPerRoom());
        assertEquals(60, response.getMaxBookingRange());
        assertEquals(0, response.getProperties().size());
        assertTrue(response.getGuests().isNull());
    }

    @Test
    void getTenantConfigWithPropertiesReturnsNullForMissingOptionalConfigValues() {
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setTenantName("radisson");
        tenant.setConfigJson("{\"guests\":{\"kids\":\"0-12\"}}");

        when(tenantRepository.findByTenantName("radisson")).thenReturn(Optional.of(tenant));
        when(propertyRepository.findByTenant_TenantId(tenantId)).thenReturn(List.of());

        TenantConfigResponse response = tenantService.getTenantConfigWithProperties("radisson");

        assertNull(response.getTenantImage());
        assertNull(response.getTenantLogo());
        assertNull(response.getMaxRooms());
        assertNull(response.getAccessibility());
        assertNull(response.getMaxCapacityPerRoom());
        assertNull(response.getMaxBookingRange());
    }
}
