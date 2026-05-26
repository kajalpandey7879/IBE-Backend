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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;

    public UUID parseUuid(String value, String fieldName) {
        requireNotBlank(value, fieldName);

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUuidFormatException(fieldName + " must be a valid UUID", ex);
        }
    }

    public void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
    }

    public void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    public void validateMinimum(Integer value, int min, String fieldName) {
        requireNotNull(value, fieldName);
        if (value < min) {
            throw new BadRequestException(fieldName + " must be at least " + min);
        }
    }

    public void validateDateRangeStrict(LocalDate startDate, LocalDate endDate, String startFieldName,
            String endFieldName) {
        requireNotNull(startDate, startFieldName);
        requireNotNull(endDate, endFieldName);
        if (!startDate.isBefore(endDate)) {
            throw new BadRequestException(startFieldName + " must be before " + endFieldName);
        }
    }

    public void validateDateRangeInclusive(LocalDate startDate, LocalDate endDate, String startFieldName,
            String endFieldName) {
        requireNotNull(startDate, startFieldName);
        requireNotNull(endDate, endFieldName);
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException(startFieldName + " must be before or equal to " + endFieldName);
        }
    }

    public void validateTenantExists(UUID tenantId) {
        requireNotNull(tenantId, "tenantId");
        try {
            if (!tenantRepository.existsById(tenantId)) {
                throw new TenantNotFoundException("Tenant not found: " + tenantId);
            }
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to validate tenant", ex);
        }
    }

    public void validatePropertyExists(UUID propertyId) {
        requireNotNull(propertyId, "propertyId");
        boolean exists;
        try {
            exists = propertyRepository.existsById(propertyId);
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to validate property", ex);
        }

        if (!exists) {
            throw new PropertyNotFoundException("Property not found: " + propertyId);
        }
    }

    public void validatePropertyBelongsToTenant(UUID propertyId, UUID tenantId) {
        requireNotNull(propertyId, "propertyId");
        requireNotNull(tenantId, "tenantId");
        try {
            propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)
                    .orElseThrow(() -> new PropertyNotFoundException(
                            "Property not found or doesn't belong to tenant: " + propertyId));
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to validate tenant/property ownership", ex);
        }
    }

    public void validateTenantAndPropertyOwnership(UUID tenantId, UUID propertyId) {
        validateTenantExists(tenantId);
        validatePropertyBelongsToTenant(propertyId, tenantId);
    }

    public RoomType validateRoomTypeBelongsToProperty(UUID roomTypeId, UUID propertyId) {
        requireNotNull(roomTypeId, "roomTypeId");
        requireNotNull(propertyId, "propertyId");
        try {
            return roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId)
                    .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found: " + roomTypeId));
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to validate roomType/property ownership", ex);
        }
    }
}
