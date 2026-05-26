package com.example.ibe.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingInputDTO {
    @jakarta.validation.constraints.NotBlank(message = "First name is required")
    private String firstName;
    
    @jakarta.validation.constraints.NotBlank(message = "Last name is required")
    private String lastName;
    
    @jakarta.validation.constraints.NotBlank(message = "Address Line 1 is required")
    private String address1;
    
    private String address2;
    
    @jakarta.validation.constraints.NotBlank(message = "Country is required")
    private String country;
    
    @jakarta.validation.constraints.NotBlank(message = "State is required")
    private String state;
    
    @jakarta.validation.constraints.NotBlank(message = "City is required")
    private String city;
    
    @jakarta.validation.constraints.NotBlank(message = "ZIP code is required")
    private String zip;
    
    @jakarta.validation.constraints.NotBlank(message = "Phone number is required")
    private String phone;
    
    @jakarta.validation.constraints.NotBlank(message = "Email is required")
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;
}
