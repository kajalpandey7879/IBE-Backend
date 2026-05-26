package com.example.ibe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StayPricingResult {
    private String appliedPackage;
    private BigDecimal nightlyRate;
    private List<RateBreakdownEntry> rateEntries;
    private BigDecimal subtotal;
    private BigDecimal taxesAndCharges;
    private BigDecimal dueNow;
    private BigDecimal dueAtResort;
    private AppliedPromotion appliedPromotion;
}
