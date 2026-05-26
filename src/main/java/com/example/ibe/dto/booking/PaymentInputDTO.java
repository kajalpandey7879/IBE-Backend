package com.example.ibe.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInputDTO {
    @jakarta.validation.constraints.NotBlank(message = "Card number is required")
    @jakarta.validation.constraints.Pattern(regexp = "^\\d{12,19}$", message = "Invalid card number format")
    private String cardNumber;
    
    @jakarta.validation.constraints.NotBlank(message = "Expiration month is required")
    @jakarta.validation.constraints.Pattern(regexp = "^(0?[1-9]|1[0-2])$", message = "Invalid expiration month")
    private String expMonth;
    
    @jakarta.validation.constraints.NotBlank(message = "Expiration year is required")
    @jakarta.validation.constraints.Pattern(regexp = "^\\d{4}$", message = "Invalid expiration year (4 digits)")
    private String expYear;
}
