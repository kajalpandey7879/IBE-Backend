package com.example.ibe.service;

import com.example.ibe.dto.PromoCodeDTO;
import com.example.ibe.dto.PromoCodeResponseDTO;
import com.example.ibe.entity.PromoCode;
import com.example.ibe.repository.PromoCodeRepository;
import com.example.ibe.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoService {

    private final PropertyRepository propertyRepository;
    private final PromoCodeRepository promoCodeRepository;

    /**
     * Validates a promo code for the given tenant and property and returns the
     * matching discount details when the code is valid.
     *
     * @param request the promo validation request containing tenant, property, and promo code
     * @return the promo validation result with discount details when found, or an invalid response otherwise
     */
    public PromoCodeResponseDTO validatePromoCode(PromoCodeDTO request) {
        UUID tenantId = request.getTenantId();
        UUID propertyId = request.getPropertyId();
        String promoCode = request.getPromoCode();

        log.info("Validating promo code '{}' for property '{}' and tenant '{}'", promoCode, propertyId, tenantId);

        boolean propertyExists = propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId).isPresent();
        if (!propertyExists) {
            log.warn("Promo validation failed: Property '{}' does not exist for tenant '{}'", propertyId, tenantId);
            return new PromoCodeResponseDTO(false, null, null, promoCode, 0);
        }

        log.debug("Property '{}' for tenant '{}' exists. Searching for promo code '{}'", propertyId, tenantId, promoCode);

        PromoCode promo = promoCodeRepository
                .findByProperty_PropertyIdAndPromoCode(propertyId, promoCode)
                .orElse(null);

        if (promo == null) {
            log.warn("Promo validation failed: Promo code '{}' not found for property '{}'", promoCode, propertyId);
            return new PromoCodeResponseDTO(false, null, null, promoCode, 0);
        }

        log.info(
                "Promo code '{}' validated successfully for property '{}' and tenant '{}'. Discount: {}%", promoCode, propertyId, tenantId,
                promo.getDiscountPercentage()
        );

        return new PromoCodeResponseDTO(
                true,
                promo.getPromoName(),
                promo.getPromoDescription(),
                promo.getPromoCode(),
                promo.getDiscountPercentage()
        );
    }
}
