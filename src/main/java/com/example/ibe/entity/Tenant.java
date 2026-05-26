package com.example.ibe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_domain", columnNames = { "domain" })
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {
    @Id
    @GeneratedValue
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotBlank
    @Column(name = "tenant_name", nullable = false, length = 120)
    private String tenantName;

    @NotBlank
    @Column(name = "domain", nullable = false, length = 255)
    private String domain;

    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private String configJson = "{}";
}
