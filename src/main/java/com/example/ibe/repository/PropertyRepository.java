package com.example.ibe.repository;

import com.example.ibe.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByTenant_TenantId(UUID tenantId);

    Optional<Property> findByPropertyIdAndTenant_TenantId(UUID propertyId, UUID tenantId);
}
