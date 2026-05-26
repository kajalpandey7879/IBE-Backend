package com.example.ibe.controller;

import com.example.ibe.dto.booking.BookingConfirmationResponse;
import com.example.ibe.dto.booking.CreateBookingRequest;
import com.example.ibe.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.context.annotation.Import;

@GraphQlTest(BookingMutationResolver.class)
@Import(com.example.ibe.config.GraphQlConfig.class)
class BookingMutationResolverTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private BookingService bookingService;

    @Test
    void shouldCreateBooking() {
        BookingConfirmationResponse mockResponse = BookingConfirmationResponse.builder()
                .bookingId("BK-12345678")
                .status("CONFIRMED")
                .roomTypeName("Suite")
                .imageUrls(java.util.List.of("https://example.com/rooms/suite-1.jpg"))
                .build();

        when(bookingService.createBooking(any(CreateBookingRequest.class))).thenReturn(mockResponse);

        String document = """
            mutation {
              createBooking(input: {
                tenantId: "123e4567-e89b-12d3-a456-426614174000",
                propertyId: "123e4567-e89b-12d3-a456-426614174001",
                roomTypeId: "123e4567-e89b-12d3-a456-426614174002",
                checkInDate: "2026-05-10",
                checkOutDate: "2026-05-12",
                roomCount: 1,
                packageChosen: "Standard Rate",
                guests: "{ \\"adults\\": 2 }",
                traveler: { firstName: "John", lastName: "Doe", phone: "123", email: "j@test.com" },
                billing: { firstName: "John", lastName: "Doe", address1: "123 St", country: "US", state: "NY", city: "NYC", zip: "10001", phone: "123", email: "j@test.com" },
                payment: { cardNumber: "4111222233334444", expMonth: "12", expYear: "2030" },
                pricing: { subtotal: 200, taxesAndCharges: 30, dueAtResort: 0, dueNow: 230, currency: "USD" }
              }) {
                bookingId
                status
                roomTypeName
                imageUrls
              }
            }
        """;

        graphQlTester.document(document)
                .execute()
                .path("createBooking.bookingId").entity(String.class).isEqualTo("BK-12345678")
                .path("createBooking.status").entity(String.class).isEqualTo("CONFIRMED")
                .path("createBooking.roomTypeName").entity(String.class).isEqualTo("Suite")
                .path("createBooking.imageUrls[0]").entity(String.class).isEqualTo("https://example.com/rooms/suite-1.jpg");
    }
}
