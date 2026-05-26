package com.example.ibe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.example.ibe.dto.enums.FilterType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyFilterDefinition {

    private UUID filterId;
    private String filterName;
    private FilterType filterType;
    private PropertyRangeFilterConfig rangeConfig;
    @Builder.Default
    private java.util.List<FilterOption> options = java.util.List.of();
}