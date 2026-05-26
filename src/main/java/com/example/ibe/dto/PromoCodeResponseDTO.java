package com.example.ibe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PromoCodeResponseDTO {

    private boolean valid;

    private String promoName;

    private String promoDescription;

    private String promoCode;

    private float discountPercentage;
}