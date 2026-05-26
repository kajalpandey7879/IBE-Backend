package com.example.ibe.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmationResponse {
    private String bookingId;
    private String status;
    private String roomTypeName;
    private List<String> imageUrls;
    private Object guests;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private TravelerResponseDTO traveler;
    private BillingResponseDTO billing;
    private PaymentResponseDTO payment;
    private PricingResponseDTO pricing;
}
