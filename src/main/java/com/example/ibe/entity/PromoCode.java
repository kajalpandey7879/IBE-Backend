package com.example.ibe.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "promocodes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue
    @Column(name = "promo_id", nullable = false)
    private UUID promoId;

    @NotBlank
    @Column(name = "promo_name", nullable = false)
    private String promoName;

    @NotBlank
    @Size(max = 200)
    @Column(name = "promo_description", nullable = false)
    private String promoDescription;

    @NotBlank
    @Size(min = 5, max = 5)
    @Column(name = "promo_code", nullable = false, length = 5)
    private String promoCode;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "discount_percentage", nullable = false)
    private float discountPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}
