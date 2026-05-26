package com.example.ibe.controller;

import com.example.ibe.dto.AvailableRoomTypesRequest;
import com.example.ibe.dto.AvailableRoomTypesResponse;
import com.example.ibe.dto.CheckoutItineraryResponse;
import com.example.ibe.dto.FetchCheckoutItineraryRequest;
import com.example.ibe.dto.RoomTypeDetailResponse;
import com.example.ibe.service.RoomSearchService;
import com.example.ibe.service.RoomTypeService;
import com.example.ibe.service.ValidationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@RequiredArgsConstructor
@Validated
public class RoomTypeQueryResolver {

    private final RoomTypeService roomTypeService;
    private final RoomSearchService roomSearchService;
    private final ValidationService validationService;

    @QueryMapping
    public AvailableRoomTypesResponse availableRoomTypes(
            @Argument("request") @Valid AvailableRoomTypesRequest request) {
        return roomSearchService.getAvailableRoomTypes(request);
    }

    @QueryMapping
    public RoomTypeDetailResponse roomTypeDetail(
        @Argument("tenantId") @NotBlank(message = "tenantId must not be blank") String tenantId,
        @Argument("propertyId") @NotBlank(message = "propertyId must not be blank") String propertyId,
        @Argument("roomTypeId") @NotBlank(message = "roomTypeId must not be blank") String roomTypeId,
        @Argument("checkInDate") java.time.LocalDate checkInDate,
        @Argument("checkOutDate") java.time.LocalDate checkOutDate,
        @Argument("requiredRooms") @Min(1) int requiredRooms
    ) {
        return roomTypeService.getRoomTypeDetail(
            validationService.parseUuid(tenantId,   "tenantId"),
            validationService.parseUuid(propertyId, "propertyId"),
            validationService.parseUuid(roomTypeId, "roomTypeId"),
            checkInDate,
            checkOutDate,
            requiredRooms);
    }

    @QueryMapping
    public CheckoutItineraryResponse fetchCheckoutItinerary(
            @Argument("args") @Valid FetchCheckoutItineraryRequest request) {
        return roomTypeService.fetchCheckoutItinerary(request);
    }
}
