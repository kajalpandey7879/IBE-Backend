package com.example.ibe.controller;

import com.example.ibe.dto.booking.BookingConfirmationResponse;
import com.example.ibe.dto.booking.CreateBookingRequest;
import com.example.ibe.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BookingMutationResolver {

    private final BookingService bookingService;

    @MutationMapping("createBooking")
    public BookingConfirmationResponse createBooking(@jakarta.validation.Valid @Argument("input") CreateBookingRequest input) {
        log.info("Received createBooking GraphQL mutation for propertyId: {}", input.getPropertyId());
        return bookingService.createBooking(input);
    }
}
