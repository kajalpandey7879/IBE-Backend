package com.example.ibe.service.filter;

import com.example.ibe.dto.AvailableRoomTypeSummary;

import java.util.List;

public interface RoomTypeFilter {

    List<AvailableRoomTypeSummary> apply(List<AvailableRoomTypeSummary> roomTypes);
}