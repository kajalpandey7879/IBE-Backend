package com.example.ibe.controller;

import com.example.ibe.dto.PromoCodeDTO;
import com.example.ibe.dto.PromoCodeResponseDTO;
import com.example.ibe.service.PromoService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PromoQueryResolver {

    private final PromoService promoService;

    @QueryMapping
    public PromoCodeResponseDTO promoDiscount(
            @Argument("tenantId") UUID tenantId,
            @Argument("propertyId") UUID propertyId,
            @Argument("promoCode") String promoCode) {
        PromoCodeDTO request = new PromoCodeDTO(tenantId, propertyId, promoCode);
        return promoService.validatePromoCode(request);
    }
}
