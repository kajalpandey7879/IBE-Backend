package com.example.ibe.service;

import com.example.ibe.dto.booking.CreateBookingRequest;
import com.example.ibe.exception.PriceChangedException;
import com.example.ibe.entity.RoomType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class PricingService {

    public BookingPriceCalculation calculatePricing(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int roomCount) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = roomType.getBasePrice()
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(roomCount));

        BigDecimal taxesAndCharges = percentageOf(
                subtotal,
                roomType.getProperty().getOccupancyTaxPercentage());

        BigDecimal currentFinalDue = subtotal.add(taxesAndCharges);

        BigDecimal dueNow = percentageOf(
                currentFinalDue,
                roomType.getProperty().getDuePercentage());
        BigDecimal dueAtResort = scaleCurrency(currentFinalDue.subtract(dueNow));

        return new BookingPriceCalculation(subtotal, taxesAndCharges, dueAtResort, dueNow, "USD");
    }

    public BookingPriceCalculation reconstructPricing(
            BigDecimal subtotal,
            BigDecimal taxesAndCharges,
            BigDecimal finalAmount,
            BigDecimal duePercentage) {
        BigDecimal normalizedSubtotal = scaleCurrency(subtotal);
        BigDecimal normalizedTaxes = scaleCurrency(taxesAndCharges != null ? taxesAndCharges : BigDecimal.ZERO);
        BigDecimal normalizedFinalAmount = finalAmount != null
                ? scaleCurrency(finalAmount)
                : scaleCurrency(normalizedSubtotal.add(normalizedTaxes));
        BigDecimal dueNow = percentageOf(normalizedFinalAmount, duePercentage);
        BigDecimal dueAtResort = scaleCurrency(normalizedFinalAmount.subtract(dueNow));

        return new BookingPriceCalculation(
                normalizedSubtotal,
                normalizedTaxes,
                dueAtResort,
                dueNow,
                "USD");
    }

    public void validateClientPricing(CreateBookingRequest request, BookingPriceCalculation calculatedPrice) {
        if (calculatedPrice.subtotal().compareTo(request.getPricing().getSubtotal()) != 0
                || calculatedPrice.taxesAndCharges().compareTo(request.getPricing().getTaxesAndCharges()) != 0
                || calculatedPrice.dueAtResort().compareTo(request.getPricing().getDueAtResort()) != 0
                || calculatedPrice.dueNow().compareTo(request.getPricing().getDueNow()) != 0
                || !calculatedPrice.currency().equalsIgnoreCase(request.getPricing().getCurrency())) {
            log.warn(
                    "Price mismatch. Expected subtotal={}, taxes={}, dueAtResort={}, dueNow={}, currency={}; Got subtotal={}, taxes={}, dueAtResort={}, dueNow={}, currency={}",
                    calculatedPrice.subtotal(),
                    calculatedPrice.taxesAndCharges(),
                    calculatedPrice.dueAtResort(),
                    calculatedPrice.dueNow(),
                    calculatedPrice.currency(),
                    request.getPricing().getSubtotal(),
                    request.getPricing().getTaxesAndCharges(),
                    request.getPricing().getDueAtResort(),
                    request.getPricing().getDueNow(),
                    request.getPricing().getCurrency());
            throw new PriceChangedException("PRICE_CHANGED: The room price has changed. Please refresh and try again.");
        }
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return scaleCurrency(amount.multiply(percentage).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
    }

    private BigDecimal scaleCurrency(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public record BookingPriceCalculation(
        BigDecimal subtotal,
        BigDecimal taxesAndCharges,
        BigDecimal dueAtResort,
        BigDecimal dueNow,
        String currency
    ) {}
}
