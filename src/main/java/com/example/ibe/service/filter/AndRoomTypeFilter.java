package com.example.ibe.service.filter;

import com.example.ibe.dto.AvailableRoomTypeSummary;

import java.util.List;

public class AndRoomTypeFilter implements RoomTypeFilter {

    private final List<RoomTypeFilter> filters;

    public AndRoomTypeFilter(List<RoomTypeFilter> filters) {
        this.filters = filters;
    }

    @Override
    public List<AvailableRoomTypeSummary> apply(List<AvailableRoomTypeSummary> roomTypes) {
        List<AvailableRoomTypeSummary> filtered = roomTypes;
        for (RoomTypeFilter filter : filters) {
            filtered = filter.apply(filtered);
        }
        return filtered;
    }
}