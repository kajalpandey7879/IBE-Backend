package com.example.ibe.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingResponseDTO {
    private String firstName;
    private String lastName;
    private String address1;
    private String address2;
    private String country;
    private String state;
    private String city;
    private String zip;
    private String phone;
    private String email;
}
