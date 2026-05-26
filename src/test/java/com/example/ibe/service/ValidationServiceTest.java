package com.example.ibe.service;

import com.example.ibe.entity.RoomType;
import com.example.ibe.exception.BadRequestException;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.exception.InvalidUuidFormatException;
import com.example.ibe.exception.PropertyNotFoundException;
import com.example.ibe.exception.RoomTypeNotFoundException;
import com.example.ibe.exception.TenantNotFoundException;
import com.example.ibe.repository.PropertyRepository;
import com.example.ibe.repository.RoomTypeRepository;
import com.example.ibe.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private ValidationService validationService;

    @Test
    void parseUuidReturnsUuidWhenValid() {
        UUID parsed = validationService.parseUuid("11111111-1111-1111-1111-111111111111", "tenantId");

        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), parsed);
    }

    @Test
    void parseUuidThrowsBadRequestWhenBlank() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validationService.parseUuid(" ", "tenantId"));

        assertEquals("tenantId must not be blank", ex.getMessage());
    }

    @Test
    void parseUuidThrowsInvalidUuidWhenMalformed() {
        InvalidUuidFormatException ex = assertThrows(InvalidUuidFormatException.class,
                () -> validationService.parseUuid("not-a-uuid", "propertyId"));

        assertEquals("propertyId must be a valid UUID", ex.getMessage());
    }

    @Test
    void validateTenantExistsThrowsWhenMissing() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.existsById(tenantId)).thenReturn(false);

        TenantNotFoundException ex = assertThrows(TenantNotFoundException.class,
                () -> validationService.validateTenantExists(tenantId));

        assertTrue(ex.getMessage().contains("Tenant not found"));
    }

    @Test
    void validateTenantExistsWrapsDataAccessError() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.existsById(tenantId)).thenThrow(new DataRetrievalFailureException("db"));

        DataProcessingException ex = assertThrows(DataProcessingException.class,
                () -> validationService.validateTenantExists(tenantId));

        assertEquals("Failed to validate tenant", ex.getMessage());
    }

    @Test
    void validatePropertyBelongsToTenantThrowsWhenMissingOwnership() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)).thenReturn(Optional.empty());

        PropertyNotFoundException ex = assertThrows(PropertyNotFoundException.class,
                () -> validationService.validatePropertyBelongsToTenant(propertyId, tenantId));

        assertTrue(ex.getMessage().contains("doesn't belong to tenant"));
    }

    @Test
    void validatePropertyBelongsToTenantWrapsDataAccessError() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId))
                .thenThrow(new DataRetrievalFailureException("db"));

        DataProcessingException ex = assertThrows(DataProcessingException.class,
                () -> validationService.validatePropertyBelongsToTenant(propertyId, tenantId));

        assertEquals("Failed to validate tenant/property ownership", ex.getMessage());
    }

    @Test
    void validateRoomTypeBelongsToPropertyReturnsRoomTypeWhenFound() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        RoomType roomType = new RoomType();
        roomType.setRoomTypeId(roomTypeId);

        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.of(roomType));

        RoomType actual = validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId);

        assertEquals(roomTypeId, actual.getRoomTypeId());
    }

    @Test
    void validateRoomTypeBelongsToPropertyThrowsWhenMissing() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();

        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.empty());

        RoomTypeNotFoundException ex = assertThrows(RoomTypeNotFoundException.class,
                () -> validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId));

        assertTrue(ex.getMessage().contains("Room type not found"));
    }

    @Test
    void validateRoomTypeBelongsToPropertyWrapsDataAccessError() {
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();

        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenThrow(new DataRetrievalFailureException("db"));

        DataProcessingException ex = assertThrows(DataProcessingException.class,
                () -> validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId));

        assertEquals("Failed to validate roomType/property ownership", ex.getMessage());
    }
}
