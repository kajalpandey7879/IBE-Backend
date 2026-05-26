package com.example.ibe.service.filter;

import com.example.ibe.dto.AmenitySummary;
import com.example.ibe.dto.AvailableRoomTypeSummary;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AmenitiesRoomTypeFilter implements RoomTypeFilter {

    private final Set<String> selectedAmenities;

    public AmenitiesRoomTypeFilter(Set<String> selectedAmenities) {
        this.selectedAmenities = selectedAmenities;
    }

    @Override
    public List<AvailableRoomTypeSummary> apply(List<AvailableRoomTypeSummary> roomTypes) {
        if (selectedAmenities == null || selectedAmenities.isEmpty()) {
            return roomTypes;
        }

        return roomTypes.stream()
                .filter(this::containsAllSelectedAmenities)
                .toList();
    }

    private boolean containsAllSelectedAmenities(AvailableRoomTypeSummary roomType) {
        if (roomType.getAmenities() == null || roomType.getAmenities().isEmpty()) {
            return false;
        }

        Set<String> roomAmenityKeys = roomType.getAmenities().stream()
                .map(AmenitySummary::getAmenityName)
                .collect(Collectors.toCollection(HashSet::new));

        return roomAmenityKeys.containsAll(selectedAmenities);
    }
}