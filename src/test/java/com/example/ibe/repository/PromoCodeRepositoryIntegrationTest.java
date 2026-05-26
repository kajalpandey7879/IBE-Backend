package com.example.ibe.repository;

import com.example.ibe.entity.PromoCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PromoCodeRepositoryIntegrationTest {

    private static final UUID OCEAN_PROPERTY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Test
    void findByPropertyPropertyIdAndPromoCodeReturnsSeededPromo() {
        PromoCode promoCode = promoCodeRepository
                .findByProperty_PropertyIdAndPromoCode(OCEAN_PROPERTY_ID, "SUM10")
                .orElseThrow();

        assertEquals(UUID.fromString("c0000001-0000-0000-0000-000000000001"), promoCode.getPromoId());
        assertEquals("Summer Sale", promoCode.getPromoName());
        assertEquals("Flat 10% off on bookings", promoCode.getPromoDescription());
        assertEquals("SUM10", promoCode.getPromoCode());
        assertEquals(10.0f, promoCode.getDiscountPercentage());
        assertTrue(promoCode.getProperty() != null);
        assertEquals(OCEAN_PROPERTY_ID, promoCode.getProperty().getPropertyId());
    }
}
