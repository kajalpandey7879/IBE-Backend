package com.example.ibe.service;

import com.example.ibe.dto.AmenitySummary;
import com.example.ibe.dto.CheckoutItineraryResponse;
import com.example.ibe.dto.FetchCheckoutItineraryRequest;
import com.example.ibe.dto.RateBreakdown;
import com.example.ibe.dto.RoomTypeDetailResponse;
import com.example.ibe.dto.StayPricingResult;
import com.example.ibe.dto.PackageResponse;
import com.example.ibe.dto.TaxChargeEntry;
import com.example.ibe.entity.RoomType;
import com.example.ibe.entity.Property;
import com.example.ibe.exception.BadRequestException;
import com.example.ibe.exception.BookingAvailabilityException;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.exception.RoomNotAvailableException;
import com.example.ibe.exception.RoomTypeNotFoundException;
import com.example.ibe.repository.BookingAvailabilityRepository;
import com.example.ibe.repository.RoomTypeImageRepository;
import com.example.ibe.repository.RoomTypeRepository;
import com.example.ibe.repository.PropertyRepository;
import com.example.ibe.service.mapper.RoomTypeDetailMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomTypeService {

    private static final DateTimeFormatter STAY_SUMMARY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH);

    private final BookingAvailabilityRepository bookingAvailabilityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final ValidationService validationService;
    private final JsonParsingService jsonParsingService;
    private final PropertyRepository propertyRepository;
    private final StayPricingService stayPricingService;

    /**
     * Fetches detailed room type information for the requested stay after
     * validating ownership, stay dates, and room availability.
     *
     * @param tenantId the tenant identifier that owns the property
     * @param propertyId the property identifier containing the room type
     * @param roomTypeId the room type identifier to fetch
     * @param checkIn the requested check-in date
     * @param checkOut the requested check-out date
     * @param requiredRooms the number of rooms requested
     * @return the room type detail response with pricing, packages, amenities, and images
     */
    @Transactional(readOnly = true)
    public RoomTypeDetailResponse getRoomTypeDetail(UUID tenantId, UUID propertyId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int requiredRooms) {
        log.info("Fetching room details: tenantId={}, propertyId={}, roomTypeId={}, checkIn={}, checkOut={}",
                tenantId, propertyId, roomTypeId, checkIn, checkOut);

        validationService.validateTenantAndPropertyOwnership(tenantId, propertyId);
        RoomType roomType = fetchRoomType(roomTypeId, propertyId);

        long nightCount = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nightCount <= 0) {
            throw new BadRequestException("Check-out date must be after check-in date");
        }

        validateRoomTypeAvailability(propertyId, roomTypeId, checkIn, checkOut, requiredRooms, nightCount);

        Property property = propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)
            .orElseThrow(() -> new DataProcessingException("Property not found for tenant"));

        int multiplier = (int) nightCount * requiredRooms;

        List<PackageResponse> packageResponses = property.getPackages().stream()
            .filter(pkg -> {
                String name = pkg.getPackageName() != null ? pkg.getPackageName().toLowerCase() : "";
                if (name.contains("long weekend")) {
                    boolean hasFriday = false, hasSaturday = false, hasSunday = false;
                    for (LocalDate date = checkIn; !date.isAfter(checkOut.minusDays(1)); date = date.plusDays(1)) {
                        switch (date.getDayOfWeek()) {
                            case FRIDAY -> hasFriday = true;
                            case SATURDAY -> hasSaturday = true;
                            case SUNDAY -> hasSunday = true;
                            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY -> {}
                        }
                    }
                    return hasFriday && hasSaturday && hasSunday;
                }
                return true;
            })
            .map(pkg -> {
                var standardRate = roomType.getBasePrice();
                var discount = pkg.getDiscountPercentage();
                var discountedNightly = standardRate.multiply(java.math.BigDecimal.valueOf(1 - discount / 100.0));
                var total = discountedNightly.multiply(java.math.BigDecimal.valueOf(multiplier));
                return new PackageResponse(pkg.getPackageName(), pkg.getPackageDesc(), total);
            })
            .toList();

        roomType.setBasePrice(roomType.getBasePrice().multiply(java.math.BigDecimal.valueOf(multiplier)));
        return buildResponse(roomType, packageResponses);
    }

    /**
     * Builds the checkout itinerary for a selected room type, stay dates, package,
     * and room count.
     *
     * @param request the checkout itinerary request containing stay and package details
     * @return the checkout itinerary response with pricing and stay breakdown
     */
    @Transactional(readOnly = true)
    public CheckoutItineraryResponse fetchCheckoutItinerary(FetchCheckoutItineraryRequest request) {
        validateCheckoutItineraryRequest(request);

        UUID tenantId = validationService.parseUuid(request.getTenantId(), "tenantId");
        UUID propertyId = validationService.parseUuid(request.getPropertyId(), "propertyId");
        UUID roomTypeId = validationService.parseUuid(request.getRoomTypeId(), "roomTypeId");

        log.info(
                "Fetching checkout itinerary: tenantId={}, propertyId={}, roomTypeId={}, checkIn={}, checkOut={}, packageChosen={}, roomCount={}",
                tenantId,
                propertyId,
                roomTypeId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getPackageChosen(),
                request.getRoomCount());

        validationService.validateTenantAndPropertyOwnership(tenantId, propertyId);

        RoomType roomType = fetchRoomType(roomTypeId, propertyId);
        long nightCount = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        validateRoomTypeAvailability(
                propertyId,
                roomTypeId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getRoomCount(),
                nightCount);

        Property property = fetchProperty(tenantId, propertyId);
        StayPricingResult pricingResult = stayPricingService.calculateStayPricing(
                property,
                roomType.getBasePrice(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getRoomCount(),
                request.getPackageChosen());

        return CheckoutItineraryResponse.builder()
                .roomName(roomType.getRoomTypeName())
                .staySummary(buildStaySummary(
                        request.getCheckInDate(),
                        request.getCheckOutDate()))
                .appliedPackage(pricingResult.getAppliedPackage())
                .nightlyRate(pricingResult.getNightlyRate())
                .roomCount(request.getRoomCount())
                .subtotal(pricingResult.getSubtotal())
                .taxesAndCharges(pricingResult.getTaxesAndCharges())
                .dueNow(pricingResult.getDueNow())
                .dueAtResort(pricingResult.getDueAtResort())
                .appliedPromotion(pricingResult.getAppliedPromotion())
                .rateBreakdown(RateBreakdown.builder()
                        .entries(pricingResult.getRateEntries())
                        .roomTotal(pricingResult.getSubtotal())
                        .taxesAndCharges(List.of(TaxChargeEntry.builder()
                                .label("Occupancy tax")
                                .amount(pricingResult.getTaxesAndCharges())
                                .build()))
                        .dueNow(pricingResult.getDueNow())
                        .dueAtResort(pricingResult.getDueAtResort())
                        .build())
                .build();
    }

    /**
     * Validates the required inputs for checkout itinerary calculation.
     *
     * @param request the checkout itinerary request to validate
     */
    private void validateCheckoutItineraryRequest(FetchCheckoutItineraryRequest request) {
        if (request == null) {
            throw new BadRequestException("args must not be null");
        }

        if (request.getCheckInDate() != null && request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("checkInDate must be today or later");
        }

        validationService.validateDateRangeStrict(
                request.getCheckInDate(),
                request.getCheckOutDate(),
                "checkInDate",
                "checkOutDate");
        validationService.validateMinimum(request.getRoomCount(), 1, "roomCount");
        validationService.requireNotBlank(request.getPackageChosen(), "packageChosen");
    }

    /**
     * Verifies that the requested room type is available for the selected stay.
     *
     * @param propertyId the property identifier
     * @param roomTypeId the room type identifier
     * @param checkIn the requested check-in date
     * @param checkOut the requested check-out date
     * @param requiredRooms the number of rooms requested
     * @param nightCount the calculated number of nights in the stay
     */
    private void validateRoomTypeAvailability(
            UUID propertyId,
            UUID roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut,
            int requiredRooms,
            long nightCount) {
        try {
            boolean isAvailable = bookingAvailabilityRepository.isRoomTypeAvailable(
                    propertyId,
                    roomTypeId,
                    checkIn,
                    checkOut,
                    requiredRooms,
                    nightCount);

            if (!isAvailable) {
                throw new RoomNotAvailableException("Requested room type is not available for the selected dates");
            }
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to verify room type availability", ex);
        }
    }

    /**
     * Checks whether the requested room type is currently available for the given
     * stay details.
     *
     * @param propertyId the property identifier
     * @param roomTypeId the room type identifier
     * @param checkIn the requested check-in date
     * @param checkOut the requested check-out date
     * @param requiredRooms the number of rooms requested
     * @param nightCount the calculated number of nights in the stay
     * @return {@code true} when the room type is available; otherwise {@code false}
     */
    public boolean isRoomTypeAvailable(
            UUID propertyId,
            UUID roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut,
            int requiredRooms,
            long nightCount) {
        try {
            return bookingAvailabilityRepository.isRoomTypeAvailable(
                    propertyId,
                    roomTypeId,
                    checkIn,
                    checkOut,
                    requiredRooms,
                    nightCount);
        } catch (DataAccessException ex) {
            throw new BookingAvailabilityException(
                    "BOOKING_AVAILABILITY_ERROR: Unable to verify room availability right now. Please try again.",
                    ex);
        }
    }

    /**
     * Fetches a room type for the given property.
     *
     * @param roomTypeId the room type identifier
     * @param propertyId the property identifier
     * @return the matching room type entity
     */
    private RoomType fetchRoomType(UUID roomTypeId, UUID propertyId) {
        try {
            return roomTypeRepository.findByRoomTypeIdAndProperty_PropertyId(roomTypeId, propertyId)
                    .orElseThrow(() -> new RoomTypeNotFoundException(
                            "Room type not found: " + roomTypeId));
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch room type details", ex);
        }
    }

    /**
     * Fetches a property for the given tenant and property identifiers.
     *
     * @param tenantId the tenant identifier
     * @param propertyId the property identifier
     * @return the matching property entity
     */
    private Property fetchProperty(UUID tenantId, UUID propertyId) {
        try {
            return propertyRepository.findByPropertyIdAndTenant_TenantId(propertyId, tenantId)
                    .orElseThrow(() -> new DataProcessingException("Property not found for tenant"));
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch property details", ex);
        }
    }

    /**
     * Formats the stay dates into the summary string shown in checkout responses.
     *
     * @param checkIn the requested check-in date
     * @param checkOut the requested check-out date
     * @return the formatted stay summary
     */
    private String buildStaySummary(LocalDate checkIn, LocalDate checkOut) {
        return checkIn.format(STAY_SUMMARY_DATE_FORMATTER)
                + " - "
                + checkOut.format(STAY_SUMMARY_DATE_FORMATTER);
    }

    /**
     * Builds the room type detail response with parsed metadata and related assets.
     *
     * @param roomType the room type entity to map
     * @param packages the package options available for the stay
     * @return the mapped room type detail response
     */
    private RoomTypeDetailResponse buildResponse(RoomType roomType, List<PackageResponse> packages) {
        return RoomTypeDetailMapper.toResponse(
                roomType,
                parseBedTypes(roomType.getBedTypes()),
                fetchAmenities(roomType.getRoomTypeId()),
                fetchImages(roomType.getRoomTypeId()),
                packages);
    }

    /**
     * Fetches the amenity summaries associated with a room type.
     *
     * @param roomTypeId the room type identifier
     * @return the mapped amenity summaries
     */
    private List<AmenitySummary> fetchAmenities(UUID roomTypeId) {
        try {
            RoomType roomType = roomTypeRepository.getReferenceById(roomTypeId);
            return RoomTypeDetailMapper.toAmenitySummaries(roomType);
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch room type amenities", ex);
        }
    }

    /**
     * Fetches the image URLs associated with a room type.
     *
     * @param roomTypeId the room type identifier
     * @return the room type image URLs
     */
    private List<String> fetchImages(UUID roomTypeId) {
        try {
            return roomTypeImageRepository.findByRoomType_RoomTypeId(roomTypeId)
                    .stream()
                    .map(image -> image.getImageUrl())
                    .toList();
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch room type images", ex);
        }
    }

    /**
     * Parses the stored bed type JSON for room type responses.
     *
     * @param bedTypesJson the raw bed type JSON
     * @return the parsed bed type structure
     */
    private JsonNode parseBedTypes(String bedTypesJson) {
        return jsonParsingService.parseJsonNode(bedTypesJson, "Invalid bed types configuration");
    }
}
