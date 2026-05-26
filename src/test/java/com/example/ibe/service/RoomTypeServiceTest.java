package com.example.ibe.service;

import com.example.ibe.entity.Property;
import com.example.ibe.entity.RoomType;
import com.example.ibe.repository.BookingAvailabilityRepository;
import com.example.ibe.repository.PropertyRepository;
import com.example.ibe.repository.RoomTypeImageRepository;
import com.example.ibe.repository.RoomTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTest {

    @Mock
    private BookingAvailabilityRepository bookingAvailabilityRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomTypeImageRepository roomTypeImageRepository;

    @Mock
    private ValidationService validationService;

    @Mock
    private JsonParsingService jsonParsingService;

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private RoomTypeService roomTypeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().when(jsonParsingService.parseJsonNode("{\"king\":1}", "Invalid bed types configuration"))
                .thenAnswer(invocation -> objectMapper.readTree(invocation.getArgument(0, String.class)));
    }

    @Test
    void getRoomTypeDetailThrowsWhenSelectedRoomTypeIsUnavailable() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        LocalDate checkIn = LocalDate.of(2026, 3, 20);
        LocalDate checkOut = LocalDate.of(2026, 3, 23);

        RoomType roomType = buildRoomType(roomTypeId, propertyId, new BigDecimal("200.00"));

        doNothing().when(validationService).validateTenantAndPropertyOwnership(tenantId, propertyId);
        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.of(roomType));
        when(bookingAvailabilityRepository.isRoomTypeAvailable(
                propertyId,
                roomTypeId,
                checkIn,
                checkOut,
                1,
                3L))
                .thenReturn(false);

        com.example.ibe.exception.RoomNotAvailableException ex = assertThrows(com.example.ibe.exception.RoomNotAvailableException.class,
                () -> roomTypeService.getRoomTypeDetail(tenantId, propertyId, roomTypeId, checkIn, checkOut, 1));

        assertEquals("Requested room type is not available for the selected dates", ex.getMessage());
        verify(propertyRepository, never()).findByPropertyIdAndTenant_TenantId(propertyId, tenantId);
    }

    @Test
    void getRoomTypeDetailReturnsResponseWhenSelectedRoomTypeIsAvailable() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        LocalDate checkIn = LocalDate.of(2026, 3, 20);
        LocalDate checkOut = LocalDate.of(2026, 3, 23);

        Property property = new Property();
        property.setPropertyId(propertyId);
        property.setPackages(List.of());

        RoomType roomType = buildRoomType(roomTypeId, propertyId, new BigDecimal("200.00"));

        doNothing().when(validationService).validateTenantAndPropertyOwnership(tenantId, propertyId);
        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.of(roomType));
        when(bookingAvailabilityRepository.isRoomTypeAvailable(
                propertyId,
                roomTypeId,
                checkIn,
                checkOut,
                2,
                3L))
                .thenReturn(true);
        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId))
                .thenReturn(Optional.of(property));
        when(roomTypeRepository.getReferenceById(roomTypeId)).thenReturn(roomType);
        when(roomTypeImageRepository.findByRoomType_RoomTypeId(roomTypeId)).thenReturn(List.of());

        var response = roomTypeService.getRoomTypeDetail(tenantId, propertyId, roomTypeId, checkIn, checkOut, 2);

        assertEquals(propertyId, response.getPropertyId());
        assertEquals("Suite Room", response.getRoomTypeName());
        assertEquals(new BigDecimal("1200.00"), response.getStandardRate().getPrice());
    }

    private RoomType buildRoomType(UUID roomTypeId, UUID propertyId, BigDecimal basePrice) {
        Property property = new Property();
        property.setPropertyId(propertyId);

        RoomType roomType = new RoomType();
        roomType.setRoomTypeId(roomTypeId);
        roomType.setRoomTypeName("Suite Room");
        roomType.setProperty(property);
        roomType.setBasePrice(basePrice);
        roomType.setMaxOccupancy(4);
        roomType.setArea(420);
        roomType.setDescription("Ocean-facing suite");
        roomType.setBedTypes("{\"king\":1}");
        roomType.setAmenities(List.of());
        return roomType;
    }
}
