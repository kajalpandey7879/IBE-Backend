package com.example.ibe.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchCheckoutItineraryRequest {

    @NotBlank(message = "tenantId must not be blank")
    private String tenantId;

    @NotBlank(message = "propertyId must not be blank")
    private String propertyId;

    @NotBlank(message = "roomTypeId must not be blank")
    private String roomTypeId;

    @NotNull(message = "checkInDate must not be null")
    private LocalDate checkInDate;

    @NotNull(message = "checkOutDate must not be null")
    private LocalDate checkOutDate;

    @NotBlank(message = "packageChosen must not be blank")
    private String packageChosen;

    @NotNull(message = "roomCount must not be null")
    @Min(value = 1, message = "roomCount must be at least 1")
    private Integer roomCount;
}
