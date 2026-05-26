package com.example.ibe.service;

import com.example.ibe.config.AwsSesProperties;
import com.example.ibe.model.email.BookingEmailPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BookingEmailServiceTest {

    @Mock
    private SesClient sesClient;

    private AwsSesProperties awsSesProperties;
    private BookingEmailService bookingEmailService;

    @BeforeEach
    void setUp() {
        awsSesProperties = new AwsSesProperties();
        awsSesProperties.setRegion("ap-south-1");
        awsSesProperties.getSes().setFromEmail("no-reply@example.com");
        awsSesProperties.getSes().setEnabled(true);
        bookingEmailService = new BookingEmailService(sesClient, awsSesProperties);
    }

    @Test
    void sendBookingConfirmation_SendsExpectedPlainTextEmail() {
        BookingEmailPayload payload = new BookingEmailPayload(
                "guest@example.com",
                "BK123456",
                "Radisson Blu Bengaluru",
                "Executive Suite",
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 28),
                "1 adult, 1 child");

        bookingEmailService.sendBookingConfirmation(payload);

        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(requestCaptor.capture());

        SendEmailRequest request = requestCaptor.getValue();
        assertEquals("no-reply@example.com", request.source());
        assertTrue(request.destination().toAddresses().contains("guest@example.com"));
        assertEquals("Booking Confirmation - BK123456", request.message().subject().data());
        assertTrue(request.message().body().text().data().contains("Your booking is confirmed."));
        assertTrue(request.message().body().text().data().contains("Property: Radisson Blu Bengaluru"));
        assertTrue(request.message().body().text().data().contains("Guests: 1 adult, 1 child"));
    }

    @Test
    void sendBookingConfirmation_SkipsWhenDisabled() {
        awsSesProperties.getSes().setEnabled(false);

        bookingEmailService.sendBookingConfirmation(new BookingEmailPayload(
                "guest@example.com",
                "BK123456",
                "Radisson Blu Bengaluru",
                "Executive Suite",
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 28),
                "2 guests"));

        verifyNoInteractions(sesClient);
    }

    @Test
    void sendBookingConfirmation_LogsAndSwallowsSesFailure(CapturedOutput output) {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SdkClientException.create("Email address is not verified"));

        bookingEmailService.sendBookingConfirmation(new BookingEmailPayload(
                "guest@example.com",
                "BK123456",
                "Radisson Blu Bengaluru",
                "Executive Suite",
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 28),
                "2 guests"));

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
        assertTrue(output.getAll().contains("Failed to send booking confirmation email"));
        assertTrue(output.getAll().contains("Email address is not verified"));
    }
}
