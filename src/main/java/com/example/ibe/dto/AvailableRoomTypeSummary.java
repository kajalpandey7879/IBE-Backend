package com.example.ibe.dto;

import com.example.ibe.dto.enums.MealPlan;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRoomTypeSummary {

    private UUID roomTypeId;
    private String roomTypeName;
    private Integer area;
    private Integer maxOccupancy;
    private JsonNode bedTypes;
    private BigDecimal basePrice;
    private List<AmenitySummary> amenities;
    private List<String> imageUrls;
    private MealPlan mealPlan;
}