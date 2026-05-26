package com.example.ibe.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PackageResponse {
    
    private String name;
    private String desc;
    private BigDecimal price;
}
