package com.example.ibe.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import com.example.ibe.dto.enums.RoomSortType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRoomTypesRequest {

    @NotBlank(message = "tenantId must not be blank")
    private String tenantId;

    @NotBlank(message = "propertyId must not be blank")
    private String propertyId;

    @NotNull(message = "checkInDate must not be null")
    private LocalDate checkInDate;

    @NotNull(message = "checkOutDate must not be null")
    private LocalDate checkOutDate;

    @NotNull(message = "requiredRooms must not be null")
    @Min(value = 1, message = "requiredRooms must be at least 1")
    private Integer requiredRooms;

    @NotNull(message = "requestedGuests must not be null")
    @Min(value = 1, message = "requestedGuests must be at least 1")
    private Integer requestedGuests;

    @Min(value = 0, message = "page must be greater than or equal to 0")
    private Integer page;

    @Min(value = 1, message = "size must be at least 1")
    private Integer size;

    @Valid
    private List<AppliedFilterInput> appliedFilters;

    private RoomSortType sortType;
}
