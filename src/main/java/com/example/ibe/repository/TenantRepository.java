package com.example.ibe.repository;

import com.example.ibe.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository  extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByDomain(String domain);
    Optional<Tenant> findByTenantName(String tenantName);
}
