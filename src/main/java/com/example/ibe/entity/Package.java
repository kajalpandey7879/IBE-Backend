package com.example.ibe.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "packages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Package {
    
    @Id
    @GeneratedValue
    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "package_description", nullable = false)
    private String packageDesc;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "discount_percentage", nullable = false)
    private float discountPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}
