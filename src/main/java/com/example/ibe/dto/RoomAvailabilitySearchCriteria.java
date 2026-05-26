package com.example.ibe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAvailabilitySearchCriteria {

    private UUID propertyId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer requiredRooms;
    private Integer requestedGuests;
}
