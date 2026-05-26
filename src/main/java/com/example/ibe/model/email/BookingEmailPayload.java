package com.example.ibe.model.email;

import java.time.LocalDate;

public record BookingEmailPayload(
        String recipientEmail,
        String bookingId,
        String propertyName,
        String roomTypeName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String guestSummary) {
}
