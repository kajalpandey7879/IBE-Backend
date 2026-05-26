package com.example.ibe.service;

import com.example.ibe.dto.enums.PaymentStatus;
import com.example.ibe.entity.PaymentInfo;
import com.example.ibe.dto.enums.PaymentMethod;
import com.example.ibe.exception.InvalidBookingRequestException;
import com.example.ibe.exception.PaymentDeclinedException;
import com.example.ibe.exception.PaymentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PaymentProcessorService {

    /**
     * Processes a payment through the mock gateway and returns the generated
     * payment details for a successful authorization.
     *
     * @param cardNumber the card number to validate and authorize
     * @param expMonth the card expiry month
     * @param expYear the card expiry year
     * @return the populated payment information for the authorized transaction
     */
    public PaymentInfo processPayment(String cardNumber, String expMonth, String expYear) {
        log.info("Processing payment via mock gateway...");

        try {
            if (cardNumber == null || cardNumber.length() < 12) {
                throw new InvalidBookingRequestException("INVALID_PAYMENT_DETAILS: Invalid card number");
            }

            // Mock a decline for a specific test card (e.g., ends in 0000)
            if (cardNumber.endsWith("0000")) {
                throw new PaymentDeclinedException("PAYMENT_DECLINED: Card declined by bank");
            }

            String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);

            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.PAID);
            paymentInfo.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            paymentInfo.setLastFourDigits(lastFourDigits);
            paymentInfo.setCreatedAt(LocalDateTime.now());
            paymentInfo.setUpdatedAt(LocalDateTime.now());
            paymentInfo.setPaidAt(LocalDateTime.now());

            log.info("Payment authorized successfully. Last 4: {}", lastFourDigits);

            return paymentInfo;
        } catch (InvalidBookingRequestException | PaymentDeclinedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Unexpected payment processing failure", ex);
            throw new PaymentProcessingException(
                    "PAYMENT_PROCESSING_ERROR: Payment could not be processed at this time.", ex);
        }
    }
}
