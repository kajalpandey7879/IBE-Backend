package com.example.ibe.service;

import com.example.ibe.dto.AppliedPromotion;
import com.example.ibe.dto.FetchCheckoutItineraryRequest;
import com.example.ibe.dto.RateBreakdownEntry;
import com.example.ibe.dto.StayPricingResult;
import com.example.ibe.entity.Property;
import com.example.ibe.entity.RoomType;
import com.example.ibe.exception.BadRequestException;
import com.example.ibe.exception.InvalidUuidFormatException;
import com.example.ibe.exception.PropertyNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutItineraryServiceTest {

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

    @Mock
    private StayPricingService stayPricingService;

    @InjectMocks
    private RoomTypeService roomTypeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().when(jsonParsingService.parseJsonNode("{\"king\":1}", "Invalid bed types configuration"))
                .thenAnswer(invocation -> objectMapper.readTree(invocation.getArgument(0, String.class)));
    }

    @Test
    void fetchCheckoutItineraryReturnsComputedPricing() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(3);
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        FetchCheckoutItineraryRequest request = FetchCheckoutItineraryRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .roomTypeId(roomTypeId.toString())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .packageChosen("Long Weekend")
                .roomCount(2)
                .build();

        RoomType roomType = buildRoomType(roomTypeId, propertyId, new BigDecimal("200.00"));
        Property property = buildProperty(propertyId, "12.00", "25.00");
        StayPricingResult pricingResult = StayPricingResult.builder()
                .appliedPackage("Long Weekend")
                .nightlyRate(new BigDecimal("180.00"))
                .rateEntries(List.of(
                        new RateBreakdownEntry("Saturday, March 21, 2026", new BigDecimal("360.00")),
                        new RateBreakdownEntry("Sunday, March 22, 2026", new BigDecimal("360.00")),
                        new RateBreakdownEntry("Monday, March 23, 2026", new BigDecimal("400.00"))))
                .subtotal(new BigDecimal("1120.00"))
                .taxesAndCharges(new BigDecimal("134.40"))
                .dueNow(new BigDecimal("313.60"))
                .dueAtResort(new BigDecimal("940.80"))
                .appliedPromotion(new AppliedPromotion("Long Weekend", "Weekend savings"))
                .build();

        stubBasicValidation(request, tenantId, propertyId, roomTypeId);
        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.of(roomType));
        when(bookingAvailabilityRepository.isRoomTypeAvailable(
                propertyId, roomTypeId, request.getCheckInDate(), request.getCheckOutDate(), 2, 3L))
                .thenReturn(true);
        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId))
                .thenReturn(Optional.of(property));
        when(stayPricingService.calculateStayPricing(
                property,
                roomType.getBasePrice(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                2,
                "Long Weekend"))
                .thenReturn(pricingResult);

        var response = roomTypeService.fetchCheckoutItinerary(request);

        assertEquals("Suite Room", response.getRoomName());
        assertEquals(
                checkInDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, uuuu"))
                        + " - "
                        + checkOutDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, uuuu")),
                response.getStaySummary());
        assertEquals(new BigDecimal("180.00"), response.getNightlyRate());
        assertEquals(new BigDecimal("1120.00"), response.getSubtotal());
        assertEquals(new BigDecimal("134.40"), response.getTaxesAndCharges());
        assertEquals(new BigDecimal("313.60"), response.getDueNow());
        assertEquals(new BigDecimal("940.80"), response.getDueAtResort());
        assertEquals(3, response.getRateBreakdown().getEntries().size());
        assertEquals(new BigDecimal("360.00"), response.getRateBreakdown().getEntries().get(0).getAmount());
        assertEquals("Occupancy tax", response.getRateBreakdown().getTaxesAndCharges().get(0).getLabel());
        verify(stayPricingService).calculateStayPricing(
                property,
                roomType.getBasePrice(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                2,
                "Long Weekend");
    }

    @Test
    void fetchCheckoutItineraryThrowsWhenDateRangeInvalid() {
        LocalDate checkInDate = LocalDate.now().plusDays(2);
        FetchCheckoutItineraryRequest request = FetchCheckoutItineraryRequest.builder()
                .tenantId(UUID.randomUUID().toString())
                .propertyId(UUID.randomUUID().toString())
                .roomTypeId(UUID.randomUUID().toString())
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate)
                .packageChosen("Long Weekend")
                .roomCount(1)
                .build();

        doThrow(new BadRequestException("checkInDate must be before checkOutDate"))
                .when(validationService)
                .validateDateRangeStrict(request.getCheckInDate(), request.getCheckOutDate(), "checkInDate", "checkOutDate");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> roomTypeService.fetchCheckoutItinerary(request));

        assertEquals("checkInDate must be before checkOutDate", ex.getMessage());
    }

    @Test
    void fetchCheckoutItineraryThrowsWhenCheckInDateIsInPast() {
        FetchCheckoutItineraryRequest request = FetchCheckoutItineraryRequest.builder()
                .tenantId(UUID.randomUUID().toString())
                .propertyId(UUID.randomUUID().toString())
                .roomTypeId(UUID.randomUUID().toString())
                .checkInDate(LocalDate.now().minusDays(1))
                .checkOutDate(LocalDate.now().plusDays(1))
                .packageChosen("Long Weekend")
                .roomCount(1)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> roomTypeService.fetchCheckoutItinerary(request));

        assertEquals("checkInDate must be today or later", ex.getMessage());
        verify(validationService, never())
                .validateDateRangeStrict(request.getCheckInDate(), request.getCheckOutDate(), "checkInDate", "checkOutDate");
    }

    @Test
    void fetchCheckoutItineraryThrowsWhenRoomTypeIdInvalid() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        FetchCheckoutItineraryRequest request = FetchCheckoutItineraryRequest.builder()
                .tenantId(UUID.randomUUID().toString())
                .propertyId(UUID.randomUUID().toString())
                .roomTypeId("bad-room-type")
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(3))
                .packageChosen("Long Weekend")
                .roomCount(1)
                .build();

        doNothing().when(validationService)
                .validateDateRangeStrict(request.getCheckInDate(), request.getCheckOutDate(), "checkInDate", "checkOutDate");
        doNothing().when(validationService).validateMinimum(1, 1, "roomCount");
        doNothing().when(validationService).requireNotBlank("Long Weekend", "packageChosen");
        when(validationService.parseUuid(request.getTenantId(), "tenantId"))
                .thenReturn(UUID.randomUUID());
        when(validationService.parseUuid(request.getPropertyId(), "propertyId"))
                .thenReturn(UUID.randomUUID());
        when(validationService.parseUuid("bad-room-type", "roomTypeId"))
                .thenThrow(new InvalidUuidFormatException("roomTypeId must be a valid UUID",
                        new IllegalArgumentException()));

        InvalidUuidFormatException ex = assertThrows(InvalidUuidFormatException.class,
                () -> roomTypeService.fetchCheckoutItinerary(request));

        assertEquals("roomTypeId must be a valid UUID", ex.getMessage());
    }

    @Test
    void fetchCheckoutItineraryThrowsWhenRoomUnavailable() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        FetchCheckoutItineraryRequest request = baseRequest(tenantId, propertyId, roomTypeId);

        RoomType roomType = buildRoomType(roomTypeId, propertyId, new BigDecimal("200.00"));
        stubBasicValidation(request, tenantId, propertyId, roomTypeId);
        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.of(roomType));
        when(bookingAvailabilityRepository.isRoomTypeAvailable(
                propertyId, roomTypeId, request.getCheckInDate(), request.getCheckOutDate(), 1, 3L))
                .thenReturn(false);

        com.example.ibe.exception.RoomNotAvailableException ex = assertThrows(com.example.ibe.exception.RoomNotAvailableException.class,
                () -> roomTypeService.fetchCheckoutItinerary(request));

        assertEquals("Requested room type is not available for the selected dates", ex.getMessage());
        verify(propertyRepository, never()).findByPropertyIdAndTenant_TenantId(propertyId, tenantId);
    }

    @Test
    void fetchCheckoutItineraryDelegatesPricingErrors() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        FetchCheckoutItineraryRequest request = baseRequest(tenantId, propertyId, roomTypeId);

        RoomType roomType = buildRoomType(roomTypeId, propertyId, new BigDecimal("200.00"));
        Property property = buildProperty(propertyId, "12.00", "25.00");
        stubBasicValidation(request, tenantId, propertyId, roomTypeId);
        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.of(roomType));
        when(bookingAvailabilityRepository.isRoomTypeAvailable(
                propertyId, roomTypeId, request.getCheckInDate(), request.getCheckOutDate(), 1, 3L))
                .thenReturn(true);
        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId))
                .thenReturn(Optional.of(property));
        when(stayPricingService.calculateStayPricing(
                property,
                roomType.getBasePrice(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                1,
                "Long Weekend"))
                .thenThrow(new BadRequestException("Selected package or promo code not found for property: Long Weekend"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> roomTypeService.fetchCheckoutItinerary(request));

        assertEquals("Selected package or promo code not found for property: Long Weekend", ex.getMessage());
    }

    @Test
    void fetchCheckoutItineraryThrowsWhenTenantPropertyMismatch() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        FetchCheckoutItineraryRequest request = baseRequest(tenantId, propertyId, roomTypeId);

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);
        doNothing().when(validationService)
                .validateDateRangeStrict(request.getCheckInDate(), request.getCheckOutDate(), "checkInDate", "checkOutDate");
        doNothing().when(validationService).validateMinimum(1, 1, "roomCount");
        doNothing().when(validationService).requireNotBlank("Long Weekend", "packageChosen");
        doThrow(new PropertyNotFoundException("Property not found or doesn't belong to tenant"))
                .when(validationService).validateTenantAndPropertyOwnership(tenantId, propertyId);

        PropertyNotFoundException ex = assertThrows(PropertyNotFoundException.class,
                () -> roomTypeService.fetchCheckoutItinerary(request));

        assertTrue(ex.getMessage().contains("doesn't belong to tenant"));
    }

    @Test
    void fetchCheckoutItineraryMapsStandardRateResult() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(3);
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        FetchCheckoutItineraryRequest request = FetchCheckoutItineraryRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .roomTypeId(roomTypeId.toString())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .packageChosen("Standard Rate")
                .roomCount(2)
                .build();

        RoomType roomType = buildRoomType(roomTypeId, propertyId, new BigDecimal("200.00"));
        Property property = buildProperty(propertyId, "12.00", "25.00");
        StayPricingResult pricingResult = StayPricingResult.builder()
                .appliedPackage("Standard Rate")
                .nightlyRate(new BigDecimal("200.00"))
                .rateEntries(List.of(
                        new RateBreakdownEntry("Saturday, March 21, 2026", new BigDecimal("400.00")),
                        new RateBreakdownEntry("Sunday, March 22, 2026", new BigDecimal("400.00")),
                        new RateBreakdownEntry("Monday, March 23, 2026", new BigDecimal("400.00"))))
                .subtotal(new BigDecimal("1200.00"))
                .taxesAndCharges(new BigDecimal("144.00"))
                .dueNow(new BigDecimal("336.00"))
                .dueAtResort(new BigDecimal("1008.00"))
                .appliedPromotion(null)
                .build();

        stubBasicValidation(request, tenantId, propertyId, roomTypeId);
        when(roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId))
                .thenReturn(Optional.of(roomType));
        when(bookingAvailabilityRepository.isRoomTypeAvailable(
                propertyId, roomTypeId, request.getCheckInDate(), request.getCheckOutDate(), 2, 3L))
                .thenReturn(true);
        when(propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId))
                .thenReturn(Optional.of(property));
        when(stayPricingService.calculateStayPricing(
                property,
                roomType.getBasePrice(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                2,
                "Standard Rate"))
                .thenReturn(pricingResult);

        var response = roomTypeService.fetchCheckoutItinerary(request);

        assertEquals("Standard Rate", response.getAppliedPackage());
        assertEquals(new BigDecimal("200.00"), response.getNightlyRate());
        assertEquals(new BigDecimal("1200.00"), response.getSubtotal());
        assertNull(response.getAppliedPromotion());
    }

    private FetchCheckoutItineraryRequest baseRequest(UUID tenantId, UUID propertyId, UUID roomTypeId) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return FetchCheckoutItineraryRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .roomTypeId(roomTypeId.toString())
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(3))
                .packageChosen("Long Weekend")
                .roomCount(1)
                .build();
    }

    private void stubBasicValidation(
            FetchCheckoutItineraryRequest request,
            UUID tenantId,
            UUID propertyId,
            UUID roomTypeId) {
        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);
        doNothing().when(validationService)
                .validateDateRangeStrict(request.getCheckInDate(), request.getCheckOutDate(), "checkInDate", "checkOutDate");
        doNothing().when(validationService).validateMinimum(request.getRoomCount(), 1, "roomCount");
        doNothing().when(validationService).requireNotBlank(request.getPackageChosen(), "packageChosen");
        doNothing().when(validationService).validateTenantAndPropertyOwnership(tenantId, propertyId);
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

    private Property buildProperty(UUID propertyId, String taxPercentage, String duePercentage) {
        Property property = new Property();
        property.setPropertyId(propertyId);
        property.setOccupancyTaxPercentage(new BigDecimal(taxPercentage));
        property.setDuePercentage(new BigDecimal(duePercentage));
        property.setPackages(List.of());
        return property;
    }
}
