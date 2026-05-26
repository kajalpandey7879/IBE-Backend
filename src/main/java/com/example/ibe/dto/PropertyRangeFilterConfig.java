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
public class PropertyRangeFilterConfig {

    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal step;
    private String unit;
    private String currency;
}