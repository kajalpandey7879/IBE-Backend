package com.example.ibe.service;

import com.example.ibe.dto.StayPricingResult;
import com.example.ibe.entity.Package;
import com.example.ibe.entity.Property;
import com.example.ibe.entity.PromoCode;
import com.example.ibe.repository.PromoCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StayPricingServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private StayPricingService stayPricingService;

    @Test
    void calculateStayPricingReturnsStandardRate() {
        Property property = buildProperty("12.00", "25.00");

        StayPricingResult result = stayPricingService.calculateStayPricing(
                property,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 24),
                2,
                "Standard Rate");

        assertEquals("Standard Rate", result.getAppliedPackage());
        assertEquals(new BigDecimal("200.00"), result.getNightlyRate());
        assertEquals(new BigDecimal("1200.00"), result.getSubtotal());
        assertEquals(new BigDecimal("144.00"), result.getTaxesAndCharges());
        assertEquals(new BigDecimal("336.00"), result.getDueNow());
        assertEquals(new BigDecimal("1008.00"), result.getDueAtResort());
        assertNull(result.getAppliedPromotion());
    }

    @Test
    void calculateStayPricingReturnsPackageDiscount() {
        Property property = buildProperty("12.00", "25.00");
        property.setPackages(List.of(buildSelectedPackage(property, "Member Deal", "Member rate", 10f)));

        StayPricingResult result = stayPricingService.calculateStayPricing(
                property,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 24),
                1,
                "Member Deal");

        assertEquals("Member Deal", result.getAppliedPackage());
        assertEquals(new BigDecimal("180.00"), result.getNightlyRate());
        assertEquals(new BigDecimal("540.00"), result.getSubtotal());
        assertEquals("Member Deal", result.getAppliedPromotion().getTitle());
    }

    @Test
    void calculateStayPricingReturnsWeekendOnlyLongWeekendBreakdown() {
        Property property = buildProperty("12.00", "25.00");
        property.setPackages(List.of(buildSelectedPackage(property, "Long Weekend", "Weekend savings", 10f)));

        StayPricingResult result = stayPricingService.calculateStayPricing(
                property,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 3, 24),
                2,
                "Long Weekend");

        assertEquals(new BigDecimal("180.00"), result.getNightlyRate());
        assertEquals(new BigDecimal("360.00"), result.getRateEntries().get(0).getAmount());
        assertEquals(new BigDecimal("360.00"), result.getRateEntries().get(1).getAmount());
        assertEquals(new BigDecimal("360.00"), result.getRateEntries().get(2).getAmount());
        assertEquals(new BigDecimal("400.00"), result.getRateEntries().get(3).getAmount());
        assertEquals(new BigDecimal("1480.00"), result.getSubtotal());
    }

    @Test
    void calculateStayPricingReturnsPromoResolvedByCode() {
        Property property = buildProperty("12.00", "25.00");
        PromoCode promoCode = buildPromoCode(property, "Summer Sale", "Flat 10% off", "SUM10", 10f);
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoCode(property.getPropertyId(), "SUM10"))
                .thenReturn(Optional.of(promoCode));

        StayPricingResult result = stayPricingService.calculateStayPricing(
                property,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 24),
                1,
                "SUM10");

        assertEquals("SUM10", result.getAppliedPackage());
        assertEquals(new BigDecimal("180.00"), result.getNightlyRate());
        assertEquals("Summer Sale", result.getAppliedPromotion().getTitle());
    }

    @Test
    void calculateStayPricingReturnsPromoResolvedByName() {
        Property property = buildProperty("12.00", "25.00");
        PromoCode promoCode = buildPromoCode(property, "Summer Sale", "Flat 10% off", "SUM10", 10f);
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoCode(property.getPropertyId(), "Summer Sale"))
                .thenReturn(Optional.empty());
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoName(property.getPropertyId(), "Summer Sale"))
                .thenReturn(Optional.of(promoCode));

        StayPricingResult result = stayPricingService.calculateStayPricing(
                property,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 24),
                1,
                "Summer Sale");

        assertEquals("Summer Sale", result.getAppliedPackage());
        assertEquals(new BigDecimal("180.00"), result.getNightlyRate());
        assertEquals("Summer Sale", result.getAppliedPromotion().getTitle());
    }

    @Test
    void calculateStayPricingThrowsWhenSelectionInvalid() {
        Property property = buildProperty("12.00", "25.00");
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoCode(property.getPropertyId(), "Unknown"))
                .thenReturn(Optional.empty());
        when(promoCodeRepository.findByProperty_PropertyIdAndPromoName(property.getPropertyId(), "Unknown"))
                .thenReturn(Optional.empty());

        com.example.ibe.exception.PromotionNotFoundException ex = assertThrows(com.example.ibe.exception.PromotionNotFoundException.class, () -> stayPricingService.calculateStayPricing(
                property,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 24),
                1,
                "Unknown"));

        assertEquals("Selected package or promo code not found for property: Unknown", ex.getMessage());
    }

    @Test
    void calculateStayPricingThrowsWhenLongWeekendPackageIneligible() {
        Property property = buildProperty("12.00", "25.00");
        property.setPackages(List.of(buildSelectedPackage(property, "Long Weekend", "Weekend savings", 10f)));

        com.example.ibe.exception.InvalidPromotionException ex = assertThrows(com.example.ibe.exception.InvalidPromotionException.class, () -> stayPricingService.calculateStayPricing(
                property,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 25),
                1,
                "Long Weekend"));

        assertEquals("Selected package is only available for stays including Friday, Saturday, and Sunday",
                ex.getMessage());
    }

    private Property buildProperty(String taxPercentage, String duePercentage) {
        Property property = new Property();
        property.setPropertyId(UUID.randomUUID());
        property.setOccupancyTaxPercentage(new BigDecimal(taxPercentage));
        property.setDuePercentage(new BigDecimal(duePercentage));
        property.setPackages(List.of());
        return property;
    }

    private Package buildSelectedPackage(Property property, String name, String description, float discountPercentage) {
        Package selectedPackage = new Package();
        selectedPackage.setPackageName(name);
        selectedPackage.setPackageDesc(description);
        selectedPackage.setDiscountPercentage(discountPercentage);
        selectedPackage.setProperty(property);
        return selectedPackage;
    }

    private PromoCode buildPromoCode(
            Property property,
            String promoName,
            String promoDescription,
            String promoCodeValue,
            float discountPercentage) {
        PromoCode promoCode = new PromoCode();
        promoCode.setProperty(property);
        promoCode.setPromoName(promoName);
        promoCode.setPromoDescription(promoDescription);
        promoCode.setPromoCode(promoCodeValue);
        promoCode.setDiscountPercentage(discountPercentage);
        return promoCode;
    }
}
