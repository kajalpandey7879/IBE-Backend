package com.example.ibe.service;

import com.example.ibe.dto.AppliedPromotion;
import com.example.ibe.dto.RateBreakdownEntry;
import com.example.ibe.dto.StayPricingResult;
import com.example.ibe.entity.Package;
import com.example.ibe.entity.PromoCode;
import com.example.ibe.entity.Property;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.exception.InvalidPromotionException;
import com.example.ibe.exception.PromotionNotFoundException;
import com.example.ibe.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StayPricingService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final String STANDARD_RATE = "Standard Rate";
    private static final DateTimeFormatter RATE_ENTRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, uuuu",
            Locale.ENGLISH);

    private final PromoCodeRepository promoCodeRepository;

    /**
     * Calculates the full stay pricing for the selected rate, including nightly
     * breakdown, taxes, due amounts, and applied promotion details.
     *
     * @param property the property providing packages, tax, and payment rules
     * @param basePrice the base nightly room price before discounts
     * @param checkIn the requested check-in date
     * @param checkOut the requested check-out date
     * @param roomCount the number of rooms included in the stay
     * @param selectedRate the selected standard rate, package name, or promo code
     * @return the calculated stay pricing result for the requested stay
     */
    public StayPricingResult calculateStayPricing(
            Property property,
            BigDecimal basePrice,
            LocalDate checkIn,
            LocalDate checkOut,
            int roomCount,
            String selectedRate) {
        Package selectedPackage = findSelectedPackage(property, selectedRate);
        PromoCode selectedPromoCode = selectedPackage == null
                ? findSelectedPromoCode(property.getPropertyId(), selectedRate)
                : null;

        if (selectedPackage == null && selectedPromoCode == null && !isStandardRate(selectedRate)) {
            throw new PromotionNotFoundException(
                    "Selected package or promo code not found for property: " + selectedRate);
        }

        if (selectedPackage != null) {
            validatePackageEligibility(selectedPackage, checkIn, checkOut);
        }

        float discountPercentage = selectedPackage != null
                ? selectedPackage.getDiscountPercentage()
                : selectedPromoCode != null ? selectedPromoCode.getDiscountPercentage() : 0f;
        boolean weekendOnlyDiscount = selectedPackage != null && isLongWeekendPackage(selectedPackage);

        BigDecimal nightlyRate = calculatePerRoomNightlyRate(
                basePrice,
                discountPercentage,
                weekendOnlyDiscount,
                checkIn);
        List<RateBreakdownEntry> rateEntries = buildRateEntries(
                checkIn,
                checkOut,
                basePrice,
                discountPercentage,
                weekendOnlyDiscount,
                roomCount);
        BigDecimal subtotal = scaleCurrency(rateEntries.stream()
                .map(RateBreakdownEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal taxesAndCharges = percentageOf(subtotal, property.getOccupancyTaxPercentage());
        BigDecimal grandTotal = subtotal.add(taxesAndCharges);
        BigDecimal dueNow = percentageOf(grandTotal, property.getDuePercentage());
        BigDecimal dueAtResort = scaleCurrency(grandTotal.subtract(dueNow));

        return StayPricingResult.builder()
                .appliedPackage(selectedRate)
                .nightlyRate(nightlyRate)
                .rateEntries(rateEntries)
                .subtotal(subtotal)
                .taxesAndCharges(taxesAndCharges)
                .dueNow(dueNow)
                .dueAtResort(dueAtResort)
                .appliedPromotion(buildAppliedPromotion(selectedPackage, selectedPromoCode))
                .build();
    }

    /**
     * Resolves the selected package from the property's configured packages when
     * the chosen rate is not the standard rate.
     *
     * @param property the property containing the available packages
     * @param packageChosen the selected rate or package name
     * @return the matching package, or {@code null} when no package applies
     */
    private Package findSelectedPackage(Property property, String packageChosen) {
        if (isStandardRate(packageChosen)) {
            return null;
        }

        List<Package> packages = property.getPackages() != null ? property.getPackages() : List.of();
        return packages.stream()
                .filter(pkg -> pkg.getPackageName().equals(packageChosen))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the selected promo code by promo code or promo name for the given
     * property when the chosen rate is not the standard rate.
     *
     * @param propertyId the property identifier
     * @param packageChosen the selected rate, promo code, or promo name
     * @return the matching promo code, or {@code null} when no promo applies
     */
    private PromoCode findSelectedPromoCode(java.util.UUID propertyId, String packageChosen) {
        if (isStandardRate(packageChosen)) {
            return null;
        }

        try {
            PromoCode promoCode = promoCodeRepository.findByProperty_PropertyIdAndPromoCode(propertyId, packageChosen)
                    .orElse(null);
            if (promoCode != null) {
                return promoCode;
            }
            return promoCodeRepository.findByProperty_PropertyIdAndPromoName(propertyId, packageChosen)
                    .orElse(null);
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch promo code details", ex);
        }
    }

    /**
     * Builds the applied promotion details for the selected package or promo code.
     *
     * @param selectedPackage the selected package, if any
     * @param selectedPromoCode the selected promo code, if any
     * @return the applied promotion details, or {@code null} when standard pricing is used
     */
    private AppliedPromotion buildAppliedPromotion(Package selectedPackage, PromoCode selectedPromoCode) {
        if (selectedPackage != null) {
            return AppliedPromotion.builder()
                    .title(selectedPackage.getPackageName())
                    .description(selectedPackage.getPackageDesc())
                    .build();
        }

        if (selectedPromoCode == null) {
            return null;
        }

        return AppliedPromotion.builder()
                .title(selectedPromoCode.getPromoName())
                .description(selectedPromoCode.getPromoDescription())
                .build();
    }

    /**
     * Validates package-specific eligibility rules before discounted pricing is
     * applied.
     *
     * @param selectedPackage the selected package to validate
     * @param checkIn the requested check-in date
     * @param checkOut the requested check-out date
     */
    private void validatePackageEligibility(Package selectedPackage, LocalDate checkIn, LocalDate checkOut) {
        if (isLongWeekendPackage(selectedPackage) && !stayIncludesLongWeekend(checkIn, checkOut)) {
            throw new InvalidPromotionException(
                    "Selected package is only available for stays including Friday, Saturday, and Sunday");
        }
    }

    private BigDecimal applyDiscount(BigDecimal basePrice, float discountPercentage) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(discountPercentage).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));
        return scaleCurrency(basePrice.multiply(discountMultiplier));
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return scaleCurrency(amount.multiply(percentage).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));
    }

    private BigDecimal scaleCurrency(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePerRoomNightlyRate(
            BigDecimal basePrice,
            float discountPercentage,
            boolean weekendOnlyDiscount,
            LocalDate date) {
        if (discountPercentage > 0 && shouldApplyDiscountOnDate(weekendOnlyDiscount, date)) {
            return applyDiscount(basePrice, discountPercentage);
        }
        return scaleCurrency(basePrice);
    }

    private List<RateBreakdownEntry> buildRateEntries(
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal basePrice,
            float discountPercentage,
            boolean weekendOnlyDiscount,
            int roomCount) {
        List<RateBreakdownEntry> entries = new ArrayList<>();
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal perRoomAmount = calculatePerRoomNightlyRate(
                    basePrice,
                    discountPercentage,
                    weekendOnlyDiscount,
                    date);
            entries.add(RateBreakdownEntry.builder()
                    .label(date.format(RATE_ENTRY_DATE_FORMATTER))
                    .amount(scaleCurrency(perRoomAmount.multiply(BigDecimal.valueOf(roomCount))))
                    .build());
        }
        return entries;
    }

    private boolean shouldApplyDiscountOnDate(boolean weekendOnlyDiscount, LocalDate date) {
        if (!weekendOnlyDiscount) {
            return true;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.FRIDAY
                || dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isStandardRate(String packageChosen) {
        return STANDARD_RATE.equalsIgnoreCase(packageChosen);
    }

    private boolean isLongWeekendPackage(Package selectedPackage) {
        String packageName = selectedPackage.getPackageName() != null
                ? selectedPackage.getPackageName().toLowerCase(Locale.ENGLISH)
                : "";
        return packageName.contains("long weekend");
    }

    private boolean stayIncludesLongWeekend(LocalDate checkIn, LocalDate checkOut) {
        boolean hasFriday = false;
        boolean hasSaturday = false;
        boolean hasSunday = false;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            switch (date.getDayOfWeek()) {
                case FRIDAY -> hasFriday = true;
                case SATURDAY -> hasSaturday = true;
                case SUNDAY -> hasSunday = true;
                default -> {
                }
            }
        }

        return hasFriday && hasSaturday && hasSunday;
    }
}
