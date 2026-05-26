package com.example.ibe.service;

import com.example.ibe.exception.DataProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JsonParsingService {

    private final ObjectMapper objectMapper;

    public JsonNode parseJsonNode(String json, String errorMessage) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            log.error(errorMessage, ex);
            throw new DataProcessingException(errorMessage, ex);
        }
    }

    public JsonNode normalizeGuestsNode(Object guests) {
        if (guests == null) {
            return objectMapper.createObjectNode();
        }

        JsonNode guestsNode = objectMapper.valueToTree(guests);
        if (!guestsNode.isTextual()) {
            return guestsNode;
        }

        String rawGuests = guestsNode.asText();
        if (rawGuests == null || rawGuests.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(rawGuests);
        } catch (JsonProcessingException ex) {
            return guestsNode;
        }
    }

    public int deriveGuestCount(JsonNode guestsNode) {
        if (guestsNode == null || guestsNode.isNull() || guestsNode.isMissingNode()) {
            return 0;
        }
        if (guestsNode.isArray()) {
            return guestsNode.size();
        }
        if (guestsNode.isNumber()) {
            return Math.max(0, guestsNode.asInt());
        }
        if (!guestsNode.isObject()) {
            return 0;
        }

        int total = sumNumericGuestCounts(guestsNode);
        if (total > 0) {
            return total;
        }

        int arrayGuestCount = sumArrayGuestCounts(guestsNode);
        if (arrayGuestCount > 0) {
            return arrayGuestCount;
        }
        return guestsNode.size();
    }

    private int sumNumericGuestCounts(JsonNode guestsNode) {
        int total = 0;
        for (String fieldName : List.of(
                "adults", "adult", "teens", "teen", "kids", "kid",
                "children", "child", "infants", "infant", "guestCount", "requestedGuests")) {
            JsonNode value = guestsNode.get(fieldName);
            if (value != null && value.isNumber()) {
                total += Math.max(0, value.asInt());
            }
        }
        return total;
    }

    private int sumArrayGuestCounts(JsonNode guestsNode) {
        int total = 0;
        Iterator<JsonNode> values = guestsNode.elements();
        while (values.hasNext()) {
            JsonNode value = values.next();
            if (value.isArray()) {
                total += value.size();
            }
        }
        return total;
    }
}
