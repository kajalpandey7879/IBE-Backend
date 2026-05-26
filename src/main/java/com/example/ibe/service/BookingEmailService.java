package com.example.ibe.service;

import com.example.ibe.config.AwsSesProperties;
import com.example.ibe.model.email.BookingEmailPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEmailService {

    private static final String UTF_8 = "UTF-8";

    private final SesClient sesClient;
    private final AwsSesProperties awsSesProperties;

    public void sendBookingConfirmation(BookingEmailPayload payload) {
        if (payload == null) {
            log.warn("Skipping booking confirmation email because payload is missing.");
            return;
        }

        if (!awsSesProperties.getSes().isEnabled()) {
            log.info("Skipping booking confirmation email because SES is disabled for bookingId={}", payload.bookingId());
            return;
        }

        if (isBlank(payload.recipientEmail())) {
            log.warn("Skipping booking confirmation email because recipient email is missing.");
            return;
        }

        if (isBlank(awsSesProperties.getSes().getFromEmail())) {
            log.warn("Skipping booking confirmation email for bookingId={} because aws.ses.from-email is not configured.",
                    payload.bookingId());
            return;
        }

        try {
            sesClient.sendEmail(buildRequest(payload));
            log.info("Booking confirmation email sent for bookingId={} to {}",
                    payload.bookingId(), payload.recipientEmail());
        } catch (SesException | SdkClientException ex) {
            log.warn("Failed to send booking confirmation email for bookingId={} to {}: {}",
                    payload.bookingId(), payload.recipientEmail(), ex.getMessage(), ex);
        }
    }

    SendEmailRequest buildRequest(BookingEmailPayload payload) {
        return SendEmailRequest.builder()
                .source(awsSesProperties.getSes().getFromEmail())
                .destination(Destination.builder().toAddresses(payload.recipientEmail()).build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .charset(UTF_8)
                                .data(buildSubject(payload))
                                .build())
                        .body(Body.builder()
                                .text(Content.builder()
                                        .charset(UTF_8)
                                        .data(buildBody(payload))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    String buildSubject(BookingEmailPayload payload) {
        return "Booking Confirmation - " + payload.bookingId();
    }

    String buildBody(BookingEmailPayload payload) {
        return String.format("""
                Your booking is confirmed.

                Booking ID: %s
                Property: %s
                Room Type: %s
                Check-in: %s
                Check-out: %s
                Guests: %s
                """,
                payload.bookingId(),
                payload.propertyName(),
                payload.roomTypeName(),
                payload.checkInDate(),
                payload.checkOutDate(),
                payload.guestSummary());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
