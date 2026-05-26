package com.example.ibe.controller;

import com.example.ibe.dto.booking.BookingConfirmationResponse;
import com.example.ibe.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@GraphQlTest(BookingQueryResolver.class)
@Import(com.example.ibe.config.GraphQlConfig.class)
class BookingQueryResolverTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private BookingService bookingService;

    @Test
    void shouldFetchBookingForTenant() {
        BookingConfirmationResponse mockResponse = BookingConfirmationResponse.builder()
                .bookingId("BK-12345678")
                .status("CONFIRMED")
                .roomTypeName("Suite")
                .imageUrls(java.util.List.of("https://example.com/rooms/suite-1.jpg"))
                .build();

        when(bookingService.getBooking(anyString(), anyString())).thenReturn(mockResponse);

        String document = """
            query {
              booking(
                tenantId: "123e4567-e89b-12d3-a456-426614174000",
                bookingId: "123e4567-e89b-12d3-a456-426614174099"
              ) {
                bookingId
                status
                roomTypeName
                imageUrls
              }
            }
        """;

        graphQlTester.document(document)
                .execute()
                .path("booking.bookingId").entity(String.class).isEqualTo("BK-12345678")
                .path("booking.status").entity(String.class).isEqualTo("CONFIRMED")
                .path("booking.roomTypeName").entity(String.class).isEqualTo("Suite")
                .path("booking.imageUrls[0]").entity(String.class).isEqualTo("https://example.com/rooms/suite-1.jpg");

        verify(bookingService).getBooking(
                "123e4567-e89b-12d3-a456-426614174000",
                "123e4567-e89b-12d3-a456-426614174099");
    }
}
