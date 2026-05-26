package com.example.ibe.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResponseDTO {
    private BigDecimal subtotal;
    private BigDecimal taxesAndCharges;
    private BigDecimal dueAtResort;
    private BigDecimal dueNow;
    private String currency;
}
