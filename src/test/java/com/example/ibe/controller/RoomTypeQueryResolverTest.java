package com.example.ibe.controller;

import com.example.ibe.dto.AvailableRoomTypesRequest;
import com.example.ibe.dto.AvailableRoomTypesResponse;
import com.example.ibe.dto.CheckoutItineraryResponse;
import com.example.ibe.dto.FetchCheckoutItineraryRequest;
import com.example.ibe.dto.PageInfo;
import com.example.ibe.dto.RoomTypeDetailResponse;
import com.example.ibe.dto.enums.MealPlan;
import com.example.ibe.exception.InvalidUuidFormatException;
import com.example.ibe.service.RoomSearchService;
import com.example.ibe.service.RoomTypeService;
import com.example.ibe.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeQueryResolverTest {

    @Mock
    private RoomTypeService roomTypeService;

    @Mock
    private RoomSearchService roomSearchService;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private RoomTypeQueryResolver resolver;

    @Test
    void availableRoomTypesDelegatesToSearchServiceWithSortType() {
        var sortType = com.example.ibe.dto.enums.RoomSortType.ASCE;
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(UUID.randomUUID().toString())
                .propertyId(UUID.randomUUID().toString())
                .checkInDate(java.time.LocalDate.now())
                .checkOutDate(java.time.LocalDate.now().plusDays(2))
                .requiredRooms(2)
                .requestedGuests(4)
                .page(0)
                .size(10)
                .sortType(sortType)
                .build();

        AvailableRoomTypesResponse expected = AvailableRoomTypesResponse.builder()
                .pageInfo(PageInfo.builder().page(0).size(10).totalElements(0).totalPages(0).hasNext(false)
                        .hasPrevious(false).build())
                .build();

        when(roomSearchService.getAvailableRoomTypes(request)).thenReturn(expected);

        AvailableRoomTypesResponse actual = resolver.availableRoomTypes(request);

        assertEquals(expected, actual);
        verify(roomSearchService).getAvailableRoomTypes(request);
    }



    @Test
    void roomTypeDetailThrowsWhenTenantIdIsInvalidUuid() {
        java.time.LocalDate checkIn = java.time.LocalDate.of(2026, 3, 20);
        java.time.LocalDate checkOut = java.time.LocalDate.of(2026, 3, 22);
        when(validationService.parseUuid("bad-tenant", "tenantId"))
                .thenThrow(new InvalidUuidFormatException("tenantId must be a valid UUID", new IllegalArgumentException()));
        InvalidUuidFormatException ex = assertThrows(
            InvalidUuidFormatException.class,
            () -> resolver.roomTypeDetail("bad-tenant", UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), checkIn, checkOut, 3));
        assertEquals("tenantId must be a valid UUID", ex.getMessage());
    }

    @Test
    void roomTypeDetailThrowsWhenPropertyIdIsInvalidUuid() {
        UUID tenantId = UUID.randomUUID();
        java.time.LocalDate checkIn = java.time.LocalDate.of(2026, 3, 20);
        java.time.LocalDate checkOut = java.time.LocalDate.of(2026, 3, 22);
        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid("bad-property", "propertyId"))
                .thenThrow(new InvalidUuidFormatException("propertyId must be a valid UUID", new IllegalArgumentException()));
        InvalidUuidFormatException ex = assertThrows(
            InvalidUuidFormatException.class,
            () -> resolver.roomTypeDetail(tenantId.toString(), "bad-property",
                UUID.randomUUID().toString(), checkIn, checkOut, 3));
        assertEquals("propertyId must be a valid UUID", ex.getMessage());
    }

    @Test
    void roomTypeDetailThrowsWhenRoomTypeIdIsInvalidUuid() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        java.time.LocalDate checkIn = java.time.LocalDate.of(2026, 3, 20);
        java.time.LocalDate checkOut = java.time.LocalDate.of(2026, 3, 22);
        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid("bad-room-type", "roomTypeId"))
                .thenThrow(new InvalidUuidFormatException("roomTypeId must be a valid UUID", new IllegalArgumentException()));
        InvalidUuidFormatException ex = assertThrows(
            InvalidUuidFormatException.class,
            () -> resolver.roomTypeDetail(tenantId.toString(), propertyId.toString(),
                "bad-room-type", checkIn, checkOut, 3));
        assertEquals("roomTypeId must be a valid UUID", ex.getMessage());
    }

        @Test
        void roomTypeDetailDelegatesToServiceForValidUuidsWithDatesAndRequiredRooms() {
                UUID tenantId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID roomTypeId = UUID.randomUUID();
                java.time.LocalDate checkIn = java.time.LocalDate.of(2026, 3, 20);
                java.time.LocalDate checkOut = java.time.LocalDate.of(2026, 3, 22);
                int requiredRooms = 3;

                RoomTypeDetailResponse expected = RoomTypeDetailResponse.builder()
                        .propertyId(propertyId)
                        .roomTypeName("Deluxe Room")
                        .mealPlan(MealPlan.HALF_BOARD)
                        .build();

                when(roomTypeService.getRoomTypeDetail(tenantId, propertyId, roomTypeId, checkIn, checkOut, requiredRooms)).thenReturn(expected);
                when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
                when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);

                RoomTypeDetailResponse actual = resolver.roomTypeDetail(
                        tenantId.toString(),
                        propertyId.toString(),
                        roomTypeId.toString(),
                        checkIn,
                        checkOut,
                        requiredRooms);

                assertEquals(expected, actual);
                verify(roomTypeService).getRoomTypeDetail(tenantId, propertyId, roomTypeId, checkIn, checkOut, requiredRooms);
        }

    @Test
    void fetchCheckoutItineraryDelegatesToService() {
        FetchCheckoutItineraryRequest request = FetchCheckoutItineraryRequest.builder()
                .tenantId(UUID.randomUUID().toString())
                .propertyId(UUID.randomUUID().toString())
                .roomTypeId(UUID.randomUUID().toString())
                .checkInDate(java.time.LocalDate.of(2026, 3, 21))
                .checkOutDate(java.time.LocalDate.of(2026, 3, 28))
                .packageChosen("Long Weekend")
                .roomCount(1)
                .build();

        CheckoutItineraryResponse expected = CheckoutItineraryResponse.builder()
                .roomName("Executive Room")
                .build();

        when(roomTypeService.fetchCheckoutItinerary(request)).thenReturn(expected);

        CheckoutItineraryResponse actual = resolver.fetchCheckoutItinerary(request);

        assertEquals(expected, actual);
        verify(roomTypeService).fetchCheckoutItinerary(request);
    }
}
