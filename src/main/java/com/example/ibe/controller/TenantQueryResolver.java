package com.example.ibe.controller;

import com.example.ibe.dto.TenantConfigResponse;
import com.example.ibe.service.TenantService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@RequiredArgsConstructor
@Validated
public class TenantQueryResolver {

    private final TenantService tenantService;

    @QueryMapping
    public TenantConfigResponse tenantConfig(
            @Argument("tenant") @NotBlank(message = "tenant must not be blank") @Size(max = 100, message = "tenant length must be <= 100") String tenant) {
        return tenantService.getTenantConfigWithProperties(tenant);
    }
}