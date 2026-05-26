package com.example.ibe.service.mapper;

import com.example.ibe.dto.AmenitySummary;
import com.example.ibe.dto.RoomTypeDetailResponse;
import com.example.ibe.dto.PackageResponse;
import com.example.ibe.dto.StandardRateResponse;
import com.example.ibe.entity.RoomType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public final class RoomTypeDetailMapper {

        private RoomTypeDetailMapper() {
        }

        public static RoomTypeDetailResponse toResponse(
                        RoomType roomType,
                        JsonNode bedTypes,
                        List<AmenitySummary> amenities,
                        List<String> imageUrls,
                        List<PackageResponse> packages) {
                return RoomTypeDetailResponse.builder()
                                .propertyId(roomType.getProperty().getPropertyId())
                                .roomTypeName(roomType.getRoomTypeName())
                                .description(roomType.getDescription())
                                .maxOccupancy(roomType.getMaxOccupancy())
                                .bedTypes(bedTypes)
                                .area(roomType.getArea())
                                .amenities(amenities)
                                .standardRate(toStandardRate(roomType))
                                .imageUrls(imageUrls)
                                .mealPlan(roomType.getMealPlan())
                                .packages(packages)
                                .build();
        }

        public static List<AmenitySummary> toAmenitySummaries(RoomType roomType) {
                return roomType.getAmenities().stream()
                                .map(amenity -> AmenitySummary.builder()
                                                .amenityId(amenity.getAmenityId())
                                                .amenityName(amenity.getAmenityName())
                                                .build())
                                .toList();
        }

        public static StandardRateResponse toStandardRate(RoomType roomType) {
                return StandardRateResponse.builder()
                                .name("Standard Rate")
                                .description("Best available rate")
                                .price(roomType.getBasePrice())
                                .build();
        }
}