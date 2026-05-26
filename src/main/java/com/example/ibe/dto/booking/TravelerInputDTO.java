package com.example.ibe.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelerInputDTO {
    @jakarta.validation.constraints.NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;
    
    @jakarta.validation.constraints.NotBlank(message = "Phone number is required")
    private String phone;
    
    @jakarta.validation.constraints.NotBlank(message = "Email is required")
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;
}
