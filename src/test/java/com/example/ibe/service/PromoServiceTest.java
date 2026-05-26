package com.example.ibe.service;

import com.example.ibe.dto.PromoCodeDTO;
import com.example.ibe.dto.PromoCodeResponseDTO;
import com.example.ibe.entity.PromoCode;
import com.example.ibe.repository.PromoCodeRepository;
import com.example.ibe.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PromoService promoService;

    @Test
    void validatePromoCodeReturnsValidResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String promoCode = "SUM10";

        PromoCodeDTO request = new PromoCodeDTO(tenantId, propertyId, promoCode);

        PromoCode promo = new PromoCode();
        promo.setPromoName("Summer Sale");
        promo.setPromoDescription("Flat 10% off");
        promo.setPromoCode(promoCode);
        promo.setDiscountPercentage(10f);

        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)).thenReturn(Optional.of(new com.example.ibe.entity.Property()));
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoCode(propertyId, promoCode))
                .thenReturn(Optional.of(promo));

        PromoCodeResponseDTO response = promoService.validatePromoCode(request);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals("Summer Sale", response.getPromoName());
        assertEquals("Flat 10% off", response.getPromoDescription());
        assertEquals("SUM10", response.getPromoCode());
        assertEquals(10, response.getDiscountPercentage());
    }

    @Test
    void validatePromoCodeReturnsInvalidWhenPropertyNotFound() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String promoCode = "SUM10";

        PromoCodeDTO request = new PromoCodeDTO(tenantId, propertyId, promoCode);

        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)).thenReturn(Optional.empty());

        PromoCodeResponseDTO response = promoService.validatePromoCode(request);

        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("SUM10", response.getPromoCode());
        assertEquals(0, response.getDiscountPercentage());
    }

    @Test
    void validatePromoCodeReturnsInvalidWhenPromoNotFound() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String promoCode = "SUM10";

        PromoCodeDTO request = new PromoCodeDTO(tenantId, propertyId, promoCode);

        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)).thenReturn(Optional.of(new com.example.ibe.entity.Property()));
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoCode(propertyId, promoCode))
                .thenReturn(Optional.empty());

        PromoCodeResponseDTO response = promoService.validatePromoCode(request);

        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("SUM10", response.getPromoCode());
        assertEquals(0, response.getDiscountPercentage());
    }

        @Test
        void validatePromoCodeThrowsWhenPropertyValidationFails() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String promoCode = "SUM10";

        PromoCodeDTO request = new PromoCodeDTO(tenantId, propertyId, promoCode);

        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId))
            .thenThrow(new DataRetrievalFailureException("db error"));

        DataRetrievalFailureException ex = assertThrows(
            DataRetrievalFailureException.class,
            () -> promoService.validatePromoCode(request)
        );

        assertTrue(ex.getMessage().contains("db error"));
        }

        @Test
        void validatePromoCodeThrowsWhenPromoLookupFails() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String promoCode = "SUM10";

        PromoCodeDTO request = new PromoCodeDTO(tenantId, propertyId, promoCode);

        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)).thenReturn(Optional.of(new com.example.ibe.entity.Property()));
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoCode(propertyId, promoCode))
            .thenThrow(new DataRetrievalFailureException("db error"));

        DataRetrievalFailureException ex = assertThrows(
            DataRetrievalFailureException.class,
            () -> promoService.validatePromoCode(request)
        );

        assertTrue(ex.getMessage().contains("db error"));
        }
}