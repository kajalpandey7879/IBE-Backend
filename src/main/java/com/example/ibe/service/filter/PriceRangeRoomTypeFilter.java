package com.example.ibe.service.filter;

import com.example.ibe.dto.AvailableRoomTypeSummary;

import java.math.BigDecimal;
import java.util.List;

public class PriceRangeRoomTypeFilter implements RoomTypeFilter {

    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;

    public PriceRangeRoomTypeFilter(BigDecimal minPrice, BigDecimal maxPrice) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    @Override
    public List<AvailableRoomTypeSummary> apply(List<AvailableRoomTypeSummary> roomTypes) {
        return roomTypes.stream()
                .filter(this::isWithinRange)
                .toList();
    }

    private boolean isWithinRange(AvailableRoomTypeSummary roomType) {
        if (roomType.getBasePrice() == null) {
            return false;
        }

        if (minPrice != null && roomType.getBasePrice().compareTo(minPrice) < 0) {
            return false;
        }

        return maxPrice == null || roomType.getBasePrice().compareTo(maxPrice) <= 0;
    }
}