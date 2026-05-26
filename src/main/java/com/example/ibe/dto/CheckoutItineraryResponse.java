package com.example.ibe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutItineraryResponse {
    private String roomName;
    private String staySummary;
    private String appliedPackage;
    private BigDecimal nightlyRate;
    private Integer roomCount;
    private BigDecimal subtotal;
    private BigDecimal taxesAndCharges;
    private BigDecimal dueNow;
    private BigDecimal dueAtResort;
    private AppliedPromotion appliedPromotion;
    private RateBreakdown rateBreakdown;
}
