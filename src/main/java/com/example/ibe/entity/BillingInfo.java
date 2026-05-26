package com.example.ibe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "billing_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillingInfo {

    @Id
    @GeneratedValue
    @Column(name = "billing_info_id", nullable = false, updatable = false)
    private UUID billingInfoId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "mailing_address_1", nullable = false, length = 255)
    private String mailingAddress1;

    @Column(name = "mailing_address_2", length = 255)
    private String mailingAddress2;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "email", nullable = false, length = 255)
    private String email;
}
