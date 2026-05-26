package com.example.ibe.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Property {
    @Id
    @GeneratedValue
    @Column(name = "property_id", nullable = false, updatable = false)
    private UUID propertyId;

    @NotBlank
    @Column(name = "property_name", nullable = false, length = 120)
    private String propertyName;

    @NotBlank
    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "helpline_number", nullable = false, length = 30)
    private String helplineNumber;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "occupancy_tax_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal occupancyTaxPercentage;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "due_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal duePercentage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RoomType> roomTypes;

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PromoCode> promoCodes;
  
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PropertyFilter> propertyFilters;

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Package> packages;
}
