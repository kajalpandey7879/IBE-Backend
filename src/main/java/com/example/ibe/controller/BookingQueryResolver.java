package com.example.ibe.controller;

import com.example.ibe.dto.booking.BookingConfirmationResponse;
import com.example.ibe.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BookingQueryResolver {

    private final BookingService bookingService;

    @QueryMapping
    public BookingConfirmationResponse booking(@Argument String tenantId, @Argument String bookingId) {
        log.info("GraphQL Query: Fetching booking with ID: {} for tenantId: {}", bookingId, tenantId);
        return bookingService.getBooking(tenantId, bookingId);
    }
}
