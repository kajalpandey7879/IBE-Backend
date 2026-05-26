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
public class RateBreakdown {
    private List<RateBreakdownEntry> entries;
    private BigDecimal roomTotal;
    private List<TaxChargeEntry> taxesAndCharges;
    private BigDecimal dueNow;
    private BigDecimal dueAtResort;
}
