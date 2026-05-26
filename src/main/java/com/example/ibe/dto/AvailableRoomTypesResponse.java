package com.example.ibe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRoomTypesResponse {

    private RoomAvailabilitySearchCriteria searchCriteria;
    private List<PropertyFilterDefinition> filters;
    private List<AvailableRoomTypeSummary> roomTypes;
    private PageInfo pageInfo;
}