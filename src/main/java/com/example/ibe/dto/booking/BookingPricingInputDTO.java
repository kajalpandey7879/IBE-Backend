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
public class BookingPricingInputDTO {
    @jakarta.validation.constraints.NotNull(message = "Subtotal is required")
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Subtotal cannot be negative")
    private BigDecimal subtotal;
    
    @jakarta.validation.constraints.NotNull(message = "Taxes and charges are required")
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Taxes cannot be negative")
    private BigDecimal taxesAndCharges;
    
    @jakarta.validation.constraints.NotNull(message = "Due at resort is required")
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Due at resort cannot be negative")
    private BigDecimal dueAtResort;
    
    @jakarta.validation.constraints.NotNull(message = "Due now is required")
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Due now cannot be negative")
    private BigDecimal dueNow;
    
    @jakarta.validation.constraints.NotBlank(message = "Currency is required")
    private String currency;
}
