package com.example.ibe.service.filter;

import com.example.ibe.dto.AvailableRoomTypeSummary;

import java.math.BigDecimal;
import java.util.List;

public class AreaRangeRoomTypeFilter implements RoomTypeFilter {

    private final BigDecimal minArea;
    private final BigDecimal maxArea;

    public AreaRangeRoomTypeFilter(BigDecimal minArea, BigDecimal maxArea) {
        this.minArea = minArea;
        this.maxArea = maxArea;
    }

    @Override
    public List<AvailableRoomTypeSummary> apply(List<AvailableRoomTypeSummary> roomTypes) {
        return roomTypes.stream()
                .filter(this::isWithinRange)
                .toList();
    }

    private boolean isWithinRange(AvailableRoomTypeSummary roomType) {
        if (roomType.getArea() == null) {
            return false;
        }

        BigDecimal area = BigDecimal.valueOf(roomType.getArea());
        if (minArea != null && area.compareTo(minArea) < 0) {
            return false;
        }

        return maxArea == null || area.compareTo(maxArea) <= 0;
    }
}