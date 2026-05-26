package com.example.ibe.service;

import com.example.ibe.dto.AppliedFilterInput;
import com.example.ibe.dto.AvailableRoomTypesRequest;
import com.example.ibe.dto.AvailableRoomTypesResponse;
import com.example.ibe.dto.RangeFilterInput;
import com.example.ibe.dto.enums.FilterType;
import com.example.ibe.dto.enums.MealPlan;
import com.example.ibe.entity.PropertyFilter;
import com.example.ibe.entity.RoomType;
import com.example.ibe.exception.BadRequestException;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.exception.InvalidUuidFormatException;
import com.example.ibe.exception.PropertyNotFoundException;
import com.example.ibe.repository.BookingAvailabilityRepository;
import com.example.ibe.repository.PropertyFilterRepository;
import com.example.ibe.repository.RoomTypeImageRepository;
import com.example.ibe.repository.RoomTypeRepository;
import com.example.ibe.entity.RoomTypeImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class RoomSearchServiceTest {

    @Mock
    private PropertyFilterRepository propertyFilterRepository;

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

        private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RoomSearchService roomSearchService;

        @BeforeEach
        void setUp() {
                lenient().when(jsonParsingService.parseJsonNode(anyString(), anyString()))
                                .thenAnswer(invocation -> objectMapper.readTree(invocation.getArgument(0, String.class)));
        }

    @Test
    void getAvailableRoomTypesReturnsMappedResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 23))
                .requiredRooms(2)
                .requestedGuests(5)
                .page(0)
                .size(10)
                .build();

        RoomType roomType = new RoomType();
        roomType.setRoomTypeId(UUID.randomUUID());
        roomType.setRoomTypeName("Deluxe Room");
        roomType.setArea(320);
        roomType.setMaxOccupancy(3);
        roomType.setBedTypes("{\"single\":2,\"double\":1}");
        roomType.setBasePrice(new BigDecimal("200.00"));
        roomType.setMealPlan(MealPlan.HALF_BOARD);

        PropertyFilter propertyFilter = new PropertyFilter();
        propertyFilter.setFilterId(UUID.randomUUID());
        propertyFilter.setFilterName("Price");
        propertyFilter.setFilterType(FilterType.RANGE);
        propertyFilter.setConfigJson("{\"min\":100,\"max\":500,\"step\":25}");

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                2,
                3L))
                .thenReturn(List.of(roomType.getRoomTypeId()));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(roomType.getRoomTypeId()),
                2,
                5))
                .thenReturn(List.of(roomType));
        when(propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId))
                .thenReturn(List.of(propertyFilter));
        
        RoomTypeImage image1 = new RoomTypeImage();
        image1.setImageUrl("https://example.com/image1.jpg");
        RoomTypeImage image2 = new RoomTypeImage();
        image2.setImageUrl("https://example.com/image2.jpg");
        when(roomTypeImageRepository.findByRoomType_RoomTypeId(roomType.getRoomTypeId()))
                .thenReturn(List.of(image1, image2));

        AvailableRoomTypesResponse response = roomSearchService.getAvailableRoomTypes(request);

        assertEquals(propertyId, response.getSearchCriteria().getPropertyId());
        assertEquals(1, response.getFilters().size());
        assertEquals("Price", response.getFilters().get(0).getFilterName());
        assertEquals(FilterType.RANGE, response.getFilters().get(0).getFilterType());
        // rangeConfig should reflect the total price for the stay (nights * rooms)
        assertEquals(new BigDecimal("600"), response.getFilters().get(0).getRangeConfig().getMin());
        assertEquals(new BigDecimal("3000"), response.getFilters().get(0).getRangeConfig().getMax());
        assertTrue(response.getFilters().get(0).getOptions().isEmpty());
        assertEquals(1, response.getRoomTypes().size());
        assertEquals("Deluxe Room", response.getRoomTypes().get(0).getRoomTypeName());
        assertEquals(2, response.getRoomTypes().get(0).getBedTypes().get("single").asInt());
        assertEquals(2, response.getRoomTypes().get(0).getImageUrls().size());
        assertEquals("https://example.com/image1.jpg", response.getRoomTypes().get(0).getImageUrls().get(0));
        assertEquals("https://example.com/image2.jpg", response.getRoomTypes().get(0).getImageUrls().get(1));
        assertEquals(MealPlan.HALF_BOARD, response.getRoomTypes().get(0).getMealPlan());
        assertEquals(1, response.getPageInfo().getTotalElements());
        verify(propertyFilterRepository).findByProperty_PropertyIdOrderByFilterNameAsc(propertyId);
        verify(roomTypeImageRepository).findByRoomType_RoomTypeId(roomType.getRoomTypeId());
    }

    @Test
    void getAvailableRoomTypesThrowsWhenRequestNull() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> roomSearchService.getAvailableRoomTypes(null));

        assertTrue(ex.getMessage().contains("request must not be null"));
    }

    @Test
    void getAvailableRoomTypesThrowsWhenCheckInIsNotBeforeCheckOut() {
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(UUID.randomUUID().toString())
                .propertyId(UUID.randomUUID().toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 20))
                .requiredRooms(1)
                .requestedGuests(1)
                .build();

        doThrow(new BadRequestException("checkInDate must be before checkOutDate"))
                .when(validationService)
                .validateDateRangeStrict(
                        request.getCheckInDate(),
                        request.getCheckOutDate(),
                        "checkInDate",
                        "checkOutDate");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> roomSearchService.getAvailableRoomTypes(request));

        assertTrue(ex.getMessage().contains("checkInDate must be before checkOutDate"));
    }

    @Test
    void getAvailableRoomTypesThrowsWhenPropertyMissing() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .requiredRooms(1)
                .requestedGuests(1)
                .build();

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        org.mockito.Mockito.doThrow(new PropertyNotFoundException("Property not found or doesn't belong to tenant"))
                .when(validationService)
                .validatePropertyBelongsToTenant(propertyId, tenantId);

        PropertyNotFoundException ex = assertThrows(
                PropertyNotFoundException.class,
                () -> roomSearchService.getAvailableRoomTypes(request));

        assertTrue(ex.getMessage().contains("doesn't belong to tenant"));
    }

    @Test
    void getAvailableRoomTypesThrowsWhenPropertyValidationFails() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .requiredRooms(1)
                .requestedGuests(1)
                .build();

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        org.mockito.Mockito.doThrow(new DataProcessingException("Failed to validate tenant/property ownership",
                new RuntimeException("db error")))
                .when(validationService)
                .validatePropertyBelongsToTenant(propertyId, tenantId);

        DataProcessingException ex = assertThrows(
                DataProcessingException.class,
                () -> roomSearchService.getAvailableRoomTypes(request));

        assertTrue(ex.getMessage().contains("Failed to validate tenant/property ownership"));
    }

    @Test
    void getAvailableRoomTypesThrowsWhenRepositoryFails() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .requiredRooms(1)
                .requestedGuests(1)
                .build();

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        UUID availableRoomTypeId = UUID.randomUUID();
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                2L))
                .thenReturn(List.of(availableRoomTypeId));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(availableRoomTypeId),
                1,
                1))
                .thenThrow(new DataRetrievalFailureException("db error"));

        DataProcessingException ex = assertThrows(
                DataProcessingException.class,
                () -> roomSearchService.getAvailableRoomTypes(request));

        assertTrue(ex.getMessage().contains("Failed to fetch available room types"));
    }

    @Test
    void getAvailableRoomTypesThrowsWhenTenantIdIsInvalidUuid() {
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId("invalid-tenant-id")
                .propertyId(UUID.randomUUID().toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .requiredRooms(1)
                .requestedGuests(1)
                .build();

        when(validationService.parseUuid("invalid-tenant-id", "tenantId"))
                .thenThrow(new InvalidUuidFormatException("tenantId must be a valid UUID",
                        new IllegalArgumentException()));

        InvalidUuidFormatException ex = assertThrows(
                InvalidUuidFormatException.class,
                () -> roomSearchService.getAvailableRoomTypes(request));

        assertEquals("tenantId must be a valid UUID", ex.getMessage());
    }

    @Test
    void getAvailableRoomTypesThrowsWhenPropertyIdIsInvalidUuid() {
        UUID tenantIdUuid = UUID.randomUUID();
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantIdUuid.toString())
                .propertyId("invalid-property-id")
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .requiredRooms(1)
                .requestedGuests(1)
                .build();

        when(validationService.parseUuid(tenantIdUuid.toString(), "tenantId")).thenReturn(tenantIdUuid);
        when(validationService.parseUuid("invalid-property-id", "propertyId"))
                .thenThrow(new InvalidUuidFormatException("propertyId must be a valid UUID",
                        new IllegalArgumentException()));

        InvalidUuidFormatException ex = assertThrows(
                InvalidUuidFormatException.class,
                () -> roomSearchService.getAvailableRoomTypes(request));

        assertEquals("propertyId must be a valid UUID", ex.getMessage());
    }

    @Test
    void getAvailableRoomTypesAppliesFiltersBeforePagination() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 23))
                .requiredRooms(1)
                .requestedGuests(1)
                .page(0)
                .size(1)
                .appliedFilters(List.of(AppliedFilterInput.builder()
                        .filterId(UUID.fromString("11111111-1111-1111-1111-111111111111").toString())
                        .selectedRange(RangeFilterInput.builder()
                                .min(new BigDecimal("200"))
                                .max(new BigDecimal("500"))
                                .build())
                        .build()))
                .build();

        RoomType economyRoom = new RoomType();
        economyRoom.setRoomTypeId(UUID.randomUUID());
        economyRoom.setRoomTypeName("Economy Room");
        economyRoom.setArea(220);
        economyRoom.setMaxOccupancy(2);
        economyRoom.setBedTypes("{\"single\":2}");
        economyRoom.setBasePrice(new BigDecimal("120.00"));

        RoomType suiteRoom = new RoomType();
        suiteRoom.setRoomTypeId(UUID.randomUUID());
        suiteRoom.setRoomTypeName("Suite Room");
        suiteRoom.setArea(420);
        suiteRoom.setMaxOccupancy(4);
        suiteRoom.setBedTypes("{\"king\":1}");
        suiteRoom.setBasePrice(new BigDecimal("420.00"));

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(
                "11111111-1111-1111-1111-111111111111",
                "appliedFilters.filterId"))
                .thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                3L))
                .thenReturn(List.of(economyRoom.getRoomTypeId(), suiteRoom.getRoomTypeId()));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(economyRoom.getRoomTypeId(), suiteRoom.getRoomTypeId()),
                1,
                1))
                .thenReturn(List.of(economyRoom, suiteRoom));
        PropertyFilter priceRangeFilter = new PropertyFilter();
        priceRangeFilter.setFilterId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        priceRangeFilter.setFilterName("Price Range");
        priceRangeFilter.setFilterType(FilterType.RANGE);
        priceRangeFilter.setConfigJson("{}");
        when(propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId))
                .thenReturn(List.of(priceRangeFilter));

        AvailableRoomTypesResponse response = roomSearchService.getAvailableRoomTypes(request);

        assertEquals(1, response.getPageInfo().getTotalElements());
        assertEquals(1, response.getRoomTypes().size());
        assertEquals("Economy Room", response.getRoomTypes().get(0).getRoomTypeName());
    }

    @Test
    void getAvailableRoomTypesAppliesAmenityFilterWithDirectValues() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String filterId = "ff000002-0000-0000-0000-000000000002";
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 23))
                .requiredRooms(1)
                .requestedGuests(1)
                .page(0)
                .size(10)
                .appliedFilters(List.of(AppliedFilterInput.builder()
                        .filterId(filterId)
                        .selectedValues(List.of("WiFi", "Mini Bar"))
                        .build()))
                .build();

        RoomType deluxeRoom = new RoomType();
        deluxeRoom.setRoomTypeId(UUID.randomUUID());
        deluxeRoom.setRoomTypeName("Deluxe Room");
        deluxeRoom.setArea(320);
        deluxeRoom.setMaxOccupancy(3);
        deluxeRoom.setBedTypes("{\"single\":2}");
        deluxeRoom.setBasePrice(new BigDecimal("200.00"));
        deluxeRoom.setAmenities(List.of(
                amenity("WiFi"),
                amenity("Mini Bar"),
                amenity("Air Conditioning")));

        RoomType suiteRoom = new RoomType();
        suiteRoom.setRoomTypeId(UUID.randomUUID());
        suiteRoom.setRoomTypeName("Suite Room");
        suiteRoom.setArea(420);
        suiteRoom.setMaxOccupancy(4);
        suiteRoom.setBedTypes("{\"king\":1}");
        suiteRoom.setBasePrice(new BigDecimal("420.00"));
        suiteRoom.setAmenities(List.of(
                amenity("WiFi"),
                amenity("Room Service")));

        PropertyFilter amenitiesFilter = new PropertyFilter();
        amenitiesFilter.setFilterId(UUID.fromString(filterId));
        amenitiesFilter.setFilterName("Amenities");
        amenitiesFilter.setFilterType(FilterType.CHECKBOX);
        amenitiesFilter.setConfigJson(
                "{\"options\":[{\"label\":\"WiFi\",\"value\":\"WiFi\"},{\"label\":\"Mini Bar\",\"value\":\"Mini Bar\"}]}");

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(filterId, "appliedFilters.filterId"))
                .thenReturn(UUID.fromString(filterId));
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                3L))
                .thenReturn(List.of(deluxeRoom.getRoomTypeId(), suiteRoom.getRoomTypeId()));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(deluxeRoom.getRoomTypeId(), suiteRoom.getRoomTypeId()),
                1,
                1))
                .thenReturn(List.of(deluxeRoom, suiteRoom));
        when(propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId))
                .thenReturn(List.of(amenitiesFilter));

        AvailableRoomTypesResponse response = roomSearchService.getAvailableRoomTypes(request);

        assertEquals(1, response.getPageInfo().getTotalElements());
        assertEquals(1, response.getRoomTypes().size());
        assertEquals("Deluxe Room", response.getRoomTypes().get(0).getRoomTypeName());
        assertEquals(2, response.getFilters().get(0).getOptions().size());
        assertEquals("WiFi", response.getFilters().get(0).getOptions().get(0).getLabel());
        assertEquals("WiFi", response.getFilters().get(0).getOptions().get(0).getValue());
    }

    @Test
    void getAvailableRoomTypesThrowsWhenRangeFilterUsesSelectedValues() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String filterId = "ff000001-0000-0000-0000-000000000001";
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 23))
                .requiredRooms(1)
                .requestedGuests(1)
                .page(0)
                .size(10)
                .appliedFilters(List.of(AppliedFilterInput.builder()
                        .filterId(filterId)
                        .selectedValues(List.of("200", "300"))
                        .build()))
                .build();

        RoomType roomType = new RoomType();
        roomType.setRoomTypeId(UUID.randomUUID());
        roomType.setRoomTypeName("Deluxe Room");
        roomType.setArea(320);
        roomType.setMaxOccupancy(3);
        roomType.setBedTypes("{\"single\":2}");
        roomType.setBasePrice(new BigDecimal("220.00"));

        PropertyFilter propertyFilter = new PropertyFilter();
        propertyFilter.setFilterId(UUID.fromString(filterId));
        propertyFilter.setFilterName("Price");
        propertyFilter.setFilterType(FilterType.RANGE);
        propertyFilter.setConfigJson("{\"min\":100,\"max\":500,\"step\":25}");

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(filterId, "appliedFilters.filterId"))
                .thenReturn(UUID.fromString(filterId));
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                3L))
                .thenReturn(List.of(roomType.getRoomTypeId()));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(roomType.getRoomTypeId()),
                1,
                1))
                .thenReturn(List.of(roomType));
        when(propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId))
                .thenReturn(List.of(propertyFilter));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> roomSearchService.getAvailableRoomTypes(request));

        assertEquals("Filter 'Price' expects selectedRange", ex.getMessage());
    }

    @Test
    void getAvailableRoomTypesAppliesAreaFilterBeforePagination() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String filterId = "ff000004-0000-0000-0000-000000000004";
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 23))
                .requiredRooms(1)
                .requestedGuests(1)
                .page(0)
                .size(10)
                .appliedFilters(List.of(AppliedFilterInput.builder()
                        .filterId(filterId)
                        .selectedRange(RangeFilterInput.builder()
                                .min(new BigDecimal("300"))
                                .max(new BigDecimal("500"))
                                .build())
                        .build()))
                .build();

        RoomType compactRoom = new RoomType();
        compactRoom.setRoomTypeId(UUID.randomUUID());
        compactRoom.setRoomTypeName("Compact Room");
        compactRoom.setArea(250);
        compactRoom.setMaxOccupancy(2);
        compactRoom.setBedTypes("{\"single\":2}");
        compactRoom.setBasePrice(new BigDecimal("180.00"));

        RoomType familyRoom = new RoomType();
        familyRoom.setRoomTypeId(UUID.randomUUID());
        familyRoom.setRoomTypeName("Family Room");
        familyRoom.setArea(420);
        familyRoom.setMaxOccupancy(4);
        familyRoom.setBedTypes("{\"queen\":1,\"single\":1}");
        familyRoom.setBasePrice(new BigDecimal("260.00"));

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(filterId, "appliedFilters.filterId"))
                .thenReturn(UUID.fromString(filterId));
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                3L))
                .thenReturn(List.of(compactRoom.getRoomTypeId(), familyRoom.getRoomTypeId()));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(compactRoom.getRoomTypeId(), familyRoom.getRoomTypeId()),
                1,
                1))
                .thenReturn(List.of(compactRoom, familyRoom));

        PropertyFilter areaFilter = new PropertyFilter();
        areaFilter.setFilterId(UUID.fromString(filterId));
        areaFilter.setFilterName("Area");
        areaFilter.setFilterType(FilterType.RANGE);
        areaFilter.setConfigJson("{}");
        when(propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId))
                .thenReturn(List.of(areaFilter));

        AvailableRoomTypesResponse response = roomSearchService.getAvailableRoomTypes(request);

        assertEquals(1, response.getPageInfo().getTotalElements());
        assertEquals(1, response.getRoomTypes().size());
        assertEquals("Family Room", response.getRoomTypes().get(0).getRoomTypeName());
    }

    @Test
    void getAvailableRoomTypesAppliesMaxOccupancyFilter() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String filterId = "ff000005-0000-0000-0000-000000000005";
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 23))
                .requiredRooms(1)
                .requestedGuests(1)
                .page(0)
                .size(10)
                .appliedFilters(List.of(AppliedFilterInput.builder()
                        .filterId(filterId)
                        .selectedRange(RangeFilterInput.builder()
                                .min(new BigDecimal("4"))
                                .max(new BigDecimal("6"))
                                .build())
                        .build()))
                .build();

        RoomType standardRoom = new RoomType();
        standardRoom.setRoomTypeId(UUID.randomUUID());
        standardRoom.setRoomTypeName("Standard Room");
        standardRoom.setArea(280);
        standardRoom.setMaxOccupancy(2);
        standardRoom.setBedTypes("{\"double\":1}");
        standardRoom.setBasePrice(new BigDecimal("180.00"));

        RoomType familyRoom = new RoomType();
        familyRoom.setRoomTypeId(UUID.randomUUID());
        familyRoom.setRoomTypeName("Family Room");
        familyRoom.setArea(420);
        familyRoom.setMaxOccupancy(5);
        familyRoom.setBedTypes("{\"queen\":1,\"single\":1}");
        familyRoom.setBasePrice(new BigDecimal("260.00"));

        PropertyFilter occupancyFilter = new PropertyFilter();
        occupancyFilter.setFilterId(UUID.fromString(filterId));
        occupancyFilter.setFilterName("Max Occupancy");
        occupancyFilter.setFilterType(FilterType.RANGE);
        occupancyFilter.setConfigJson("{}");

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(filterId, "appliedFilters.filterId"))
                .thenReturn(UUID.fromString(filterId));
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                3L))
                .thenReturn(List.of(standardRoom.getRoomTypeId(), familyRoom.getRoomTypeId()));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(standardRoom.getRoomTypeId(), familyRoom.getRoomTypeId()),
                1,
                1))
                .thenReturn(List.of(standardRoom, familyRoom));
        when(propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId))
                .thenReturn(List.of(occupancyFilter));

        AvailableRoomTypesResponse response = roomSearchService.getAvailableRoomTypes(request);

        assertEquals(1, response.getPageInfo().getTotalElements());
        assertEquals(1, response.getRoomTypes().size());
        assertEquals("Family Room", response.getRoomTypes().get(0).getRoomTypeName());
    }

    @Test
    void getAvailableRoomTypesAppliesBedTypeFilter() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        String filterId = "ff000006-0000-0000-0000-000000000006";
        AvailableRoomTypesRequest request = AvailableRoomTypesRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 23))
                .requiredRooms(1)
                .requestedGuests(1)
                .page(0)
                .size(10)
                .appliedFilters(List.of(AppliedFilterInput.builder()
                        .filterId(filterId)
                        .selectedValues(List.of("king"))
                        .build()))
                .build();

        RoomType compactRoom = new RoomType();
        compactRoom.setRoomTypeId(UUID.randomUUID());
        compactRoom.setRoomTypeName("Compact Room");
        compactRoom.setArea(240);
        compactRoom.setMaxOccupancy(2);
        compactRoom.setBedTypes("{\"double\":1}");
        compactRoom.setBasePrice(new BigDecimal("150.00"));

        RoomType suiteRoom = new RoomType();
        suiteRoom.setRoomTypeId(UUID.randomUUID());
        suiteRoom.setRoomTypeName("Suite Room");
        suiteRoom.setArea(460);
        suiteRoom.setMaxOccupancy(4);
        suiteRoom.setBedTypes("{\"king\":1,\"sofa\":1}");
        suiteRoom.setBasePrice(new BigDecimal("390.00"));

        PropertyFilter bedTypeFilter = new PropertyFilter();
        bedTypeFilter.setFilterId(UUID.fromString(filterId));
        bedTypeFilter.setFilterName("Bed Type");
        bedTypeFilter.setFilterType(FilterType.CHECKBOX);
        bedTypeFilter.setConfigJson("{}");

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(filterId, "appliedFilters.filterId"))
                .thenReturn(UUID.fromString(filterId));
        when(bookingAvailabilityRepository.findAvailableRoomTypeIds(
                propertyId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                3L))
                .thenReturn(List.of(compactRoom.getRoomTypeId(), suiteRoom.getRoomTypeId()));
        when(roomTypeRepository.findAvailableRoomTypesByIds(
                propertyId,
                List.of(compactRoom.getRoomTypeId(), suiteRoom.getRoomTypeId()),
                1,
                1))
                .thenReturn(List.of(compactRoom, suiteRoom));
        when(propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId))
                .thenReturn(List.of(bedTypeFilter));

        AvailableRoomTypesResponse response = roomSearchService.getAvailableRoomTypes(request);

        assertEquals(1, response.getPageInfo().getTotalElements());
        assertEquals(1, response.getRoomTypes().size());
        assertEquals("Suite Room", response.getRoomTypes().get(0).getRoomTypeName());
    }

    private com.example.ibe.entity.Amenity amenity(String amenityName) {
        com.example.ibe.entity.Amenity amenity = new com.example.ibe.entity.Amenity();
        amenity.setAmenityId(UUID.randomUUID());
        amenity.setAmenityName(amenityName);
        return amenity;
    }
}
