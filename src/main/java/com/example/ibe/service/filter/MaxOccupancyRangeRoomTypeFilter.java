package com.example.ibe.service.filter;

import com.example.ibe.dto.AvailableRoomTypeSummary;

import java.math.BigDecimal;
import java.util.List;

public class MaxOccupancyRangeRoomTypeFilter implements RoomTypeFilter {

    private final BigDecimal minOccupancy;
    private final BigDecimal maxOccupancy;

    public MaxOccupancyRangeRoomTypeFilter(BigDecimal minOccupancy, BigDecimal maxOccupancy) {
        this.minOccupancy = minOccupancy;
        this.maxOccupancy = maxOccupancy;
    }

    @Override
    public List<AvailableRoomTypeSummary> apply(List<AvailableRoomTypeSummary> roomTypes) {
        return roomTypes.stream()
                .filter(this::isWithinRange)
                .toList();
    }

    private boolean isWithinRange(AvailableRoomTypeSummary roomType) {
        if (roomType.getMaxOccupancy() == null) {
            return false;
        }

        BigDecimal occupancy = BigDecimal.valueOf(roomType.getMaxOccupancy());
        if (minOccupancy != null && occupancy.compareTo(minOccupancy) < 0) {
            return false;
        }

        return maxOccupancy == null || occupancy.compareTo(maxOccupancy) <= 0;
    }
}