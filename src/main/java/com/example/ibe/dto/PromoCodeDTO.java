package com.example.ibe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class PromoCodeDTO {
    @NotNull
    private UUID tenantId;

    @NotNull
    private UUID propertyId;

    @NotBlank
    @Length(min = 5, max = 5)
    private String promoCode;
}
