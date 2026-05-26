package com.example.ibe.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    @jakarta.validation.constraints.NotBlank(message = "tenantId is required")
    private String tenantId;
    
    @jakarta.validation.constraints.NotBlank(message = "propertyId is required")
    private String propertyId;
    
    @jakarta.validation.constraints.NotBlank(message = "roomTypeId is required")
    private String roomTypeId;
    
    @jakarta.validation.constraints.NotNull(message = "checkInDate is required")
    private LocalDate checkInDate;
    
    @jakarta.validation.constraints.NotNull(message = "checkOutDate is required")
    private LocalDate checkOutDate;
    
    @jakarta.validation.constraints.Min(value = 1, message = "roomCount must be at least 1")
    private Integer roomCount;

    @jakarta.validation.constraints.NotBlank(message = "packageChosen is required")
    private String packageChosen;
    
    private Object guests;

    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotNull(message = "traveler information is required")
    private TravelerInputDTO traveler;
    
    @jakarta.validation.Valid
    private BillingInputDTO billing;
    
    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotNull(message = "payment information is required")
    private PaymentInputDTO payment;
    
    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotNull(message = "pricing information is required")
    private BookingPricingInputDTO pricing;
}
