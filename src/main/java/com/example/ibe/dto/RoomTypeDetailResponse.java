package com.example.ibe.dto;

import com.example.ibe.dto.enums.MealPlan;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeDetailResponse {
    private UUID propertyId;
    private String roomTypeName;
    private String description;
    private Integer maxOccupancy;
    private JsonNode bedTypes;
    private Integer area;
    private List<AmenitySummary> amenities;
    private StandardRateResponse standardRate;
    private List<String> imageUrls;
    private MealPlan mealPlan;
    private List<PackageResponse> packages;
}
