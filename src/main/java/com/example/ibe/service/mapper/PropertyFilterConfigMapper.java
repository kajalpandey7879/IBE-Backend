package com.example.ibe.service.mapper;

import com.example.ibe.dto.FilterOption;
import com.example.ibe.dto.PropertyRangeFilterConfig;
import com.example.ibe.dto.enums.FilterType;
import com.example.ibe.exception.DataProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class PropertyFilterConfigMapper {

    private static final String INVALID_CONFIG_MESSAGE = "Invalid property filter configuration";

    private PropertyFilterConfigMapper() {
    }

    public static PropertyRangeFilterConfig mapRangeConfig(FilterType filterType, JsonNode config) {
        if (filterType != FilterType.RANGE || config == null || !config.isObject()) {
            return null;
        }

        return PropertyRangeFilterConfig.builder()
                .min(decimalValue(config, "min"))
                .max(decimalValue(config, "max"))
                .step(decimalValue(config, "step"))
                .unit(textValue(config, "unit"))
                .currency(textValue(config, "currency"))
                .build();
    }

    public static List<FilterOption> mapFilterOptions(JsonNode config) {
        if (config == null || !config.isObject()) {
            return List.of();
        }

        JsonNode optionsNode = config.get("options");
        if (optionsNode == null || !optionsNode.isArray()) {
            return List.of();
        }

        List<FilterOption> options = new ArrayList<>();
        for (JsonNode optionNode : optionsNode) {
            String label = textValue(optionNode, "label");
            String value = textValue(optionNode, "value");
            if (label != null && value != null) {
                options.add(FilterOption.builder()
                        .label(label)
                        .value(value)
                        .build());
            }
        }
        return options;
    }

    private static BigDecimal decimalValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (!valueNode.isNumber()) {
            throw new DataProcessingException(INVALID_CONFIG_MESSAGE);
        }
        return valueNode.decimalValue();
    }

    private static String textValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (!valueNode.isTextual()) {
            throw new DataProcessingException(INVALID_CONFIG_MESSAGE);
        }
        return valueNode.asText();
    }
}