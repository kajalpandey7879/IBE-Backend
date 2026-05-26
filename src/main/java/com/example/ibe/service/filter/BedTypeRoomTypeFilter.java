package com.example.ibe.service.filter;

import com.example.ibe.dto.AvailableRoomTypeSummary;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BedTypeRoomTypeFilter implements RoomTypeFilter {

    private final Set<String> selectedBedTypes;

    public BedTypeRoomTypeFilter(Set<String> selectedBedTypes) {
        this.selectedBedTypes = selectedBedTypes;
    }

    @Override
    public List<AvailableRoomTypeSummary> apply(List<AvailableRoomTypeSummary> roomTypes) {
        if (selectedBedTypes == null || selectedBedTypes.isEmpty()) {
            return roomTypes;
        }

        return roomTypes.stream()
                .filter(this::containsAnySelectedBedType)
                .toList();
    }

    private boolean containsAnySelectedBedType(AvailableRoomTypeSummary roomType) {
        JsonNode bedTypes = roomType.getBedTypes();
        if (bedTypes == null || !bedTypes.isObject()) {
            return false;
        }

        Set<String> roomBedTypeKeys = new HashSet<>();
        Iterator<String> fieldNames = bedTypes.fieldNames();
        while (fieldNames.hasNext()) {
            roomBedTypeKeys.add(normalize(fieldNames.next()));
        }

        for (String selectedBedType : selectedBedTypes) {
            if (roomBedTypeKeys.contains(normalize(selectedBedType))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}