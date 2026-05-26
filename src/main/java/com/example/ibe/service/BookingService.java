package com.example.ibe.service;

import com.example.ibe.dto.StayPricingResult;
import com.example.ibe.dto.booking.*;
import com.example.ibe.dto.enums.BookingStatus;
import com.example.ibe.entity.*;
import com.example.ibe.exception.BookingAvailabilityException;
import com.example.ibe.exception.BookingPersistenceException;
import com.example.ibe.exception.ConcurrentBookingContentionException;
import com.example.ibe.exception.InvalidBookingRequestException;
import com.example.ibe.exception.RoomNotAvailableException;
import com.example.ibe.repository.BookingRepository;
import com.example.ibe.repository.RoomRepository;
import com.example.ibe.repository.RoomTypeImageRepository;
import com.example.ibe.service.PricingService.BookingPriceCalculation;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.ibe.model.email.BookingEmailPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final ValidationService validationService;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final PricingService pricingService;
    private final StayPricingService stayPricingService;
    private final PaymentProcessorService paymentProcessorService;
    private final JsonParsingService jsonParsingService;
    private final RoomTypeService roomTypeService;
    private final BookingEmailService bookingEmailService;

    /**
     * Creates and confirms a booking for the requested room type and stay dates.
     *
     * <p>
     * This operation validates the request, verifies ownership and availability,
     * recalculates
     * pricing on the server, locks physical rooms, processes payment, persists the
     * booking, and
     * returns the confirmed booking details. Transient room-lock contention is
     * retried
     * automatically according to the configured retry policy.
     * </p>
     *
     * @param request the booking request containing tenant, property, stay, guest,
     *                traveler,
     *                billing, pricing, and payment details
     * @return the confirmed booking response
     * @throws InvalidBookingRequestException if required request data is missing or
     *                                        invalid
     * @throws RoomNotAvailableException      if the requested rooms are no longer
     *                                        available
     * @throws PriceChangedException          if the client-provided pricing no
     *                                        longer matches server pricing
     * @throws BookingAvailabilityException   if room availability cannot be
     *                                        verified or rooms cannot be reserved
     * @throws BookingPersistenceException    if the booking cannot be persisted
     */
    @Transactional
    @Retryable(retryFor = ConcurrentBookingContentionException.class, maxAttempts = 5, backoff = @Backoff(delay = 50))
    public BookingConfirmationResponse createBooking(CreateBookingRequest request) {
        if (request == null) {
            throw new InvalidBookingRequestException("INVALID_BOOKING_REQUEST: Booking request is required.");
        }

        log.info("Processing createBooking request for roomTypeId: {}", request.getRoomTypeId());

        UUID tenantId = validationService.parseUuid(request.getTenantId(), "tenantId");
        UUID propertyId = validationService.parseUuid(request.getPropertyId(), "propertyId");
        UUID roomTypeId = validationService.parseUuid(request.getRoomTypeId(), "roomTypeId");

        validationService.validateDateRangeStrict(request.getCheckInDate(), request.getCheckOutDate(), "checkInDate",
                "checkOutDate");
        validationService.validateMinimum(request.getRoomCount(), 1, "roomCount");
        validationService.requireNotBlank(request.getPackageChosen(), "packageChosen");
        int roomCount = request.getRoomCount();

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

        validationService.validateTenantAndPropertyOwnership(tenantId, propertyId);
        RoomType roomType = validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId);

        boolean available = roomTypeService.isRoomTypeAvailable(
                propertyId,
                roomTypeId,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                roomCount,
                nights);

        if (!available) {
            log.error("Room Type {} is no longer available for the requested dates.", roomTypeId);
            throw new RoomNotAvailableException("ROOM_NOT_AVAILABLE: The requested room is no longer available.");
        }

        StayPricingResult stayPricing = stayPricingService.calculateStayPricing(
                roomType.getProperty(),
                roomType.getBasePrice(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                roomCount,
                request.getPackageChosen());
        BookingPriceCalculation calculatedPrice = new BookingPriceCalculation(
                stayPricing.getSubtotal(),
                stayPricing.getTaxesAndCharges(),
                stayPricing.getDueAtResort(),
                stayPricing.getDueNow(),
                "USD");

        if (request.getPricing() == null) {
            throw new InvalidBookingRequestException("INVALID_BOOKING_REQUEST: Pricing information is missing.");
        }

        pricingService.validateClientPricing(request, calculatedPrice);

        List<UUID> lockedRoomIds = lockAvailableRooms(
                roomType.getRoomTypeId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                roomCount);

        if (request.getPayment() == null) {
            throw new InvalidBookingRequestException("INVALID_PAYMENT_DETAILS: Missing payment details.");
        }
        PaymentInfo paymentInfo = paymentProcessorService.processPayment(
                request.getPayment().getCardNumber(),
                request.getPayment().getExpMonth(),
                request.getPayment().getExpYear());

        JsonNode guestsNode = jsonParsingService.normalizeGuestsNode(request.getGuests());

        Booking booking = new Booking();
        booking.setProperty(roomType.getProperty());
        booking.setRoomType(roomType);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNoOfRooms(roomCount);
        booking.setNoOfGuests(jsonParsingService.deriveGuestCount(guestsNode));
        booking.setGuestsJson(guestsNode);
        booking.setTotalPrice(calculatedPrice.subtotal());
        booking.setTaxAmount(calculatedPrice.taxesAndCharges());
        booking.setFinalAmount(calculatedPrice.subtotal().add(calculatedPrice.taxesAndCharges()));
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        List<RoomAssignment> assignments = new java.util.ArrayList<>();
        for (UUID rId : lockedRoomIds) {
            RoomAssignment ra = new RoomAssignment();
            ra.setBooking(booking);
            Room r = new Room();
            r.setRoomId(rId);
            ra.setRoom(r);
            ra.setStartDate(request.getCheckInDate());
            ra.setEndDate(request.getCheckOutDate());
            assignments.add(ra);
        }
        booking.setRoomAssignments(assignments);

        booking.setPaymentInfo(paymentInfo);

        if (request.getTraveler() == null) {
            throw new InvalidBookingRequestException("INVALID_BOOKING_REQUEST: Traveler info is missing.");
        }
        TravellerInfo traveler = new TravellerInfo();
        traveler.setFirstName(request.getTraveler().getFirstName());
        traveler.setLastName(request.getTraveler().getLastName());
        traveler.setEmail(request.getTraveler().getEmail());
        traveler.setPhoneNumber(request.getTraveler().getPhone());
        booking.setTravellerInfo(traveler);

        if (request.getBilling() != null) {
            BillingInfo billing = new BillingInfo();
            billing.setFirstName(request.getBilling().getFirstName());
            billing.setLastName(request.getBilling().getLastName());
            billing.setMailingAddress1(request.getBilling().getAddress1());
            billing.setMailingAddress2(request.getBilling().getAddress2());
            billing.setCity(request.getBilling().getCity());
            billing.setState(request.getBilling().getState());
            billing.setCountry(request.getBilling().getCountry());
            billing.setZipCode(request.getBilling().getZip());
            billing.setEmail(request.getBilling().getEmail());
            billing.setPhoneNumber(request.getBilling().getPhone());
            booking.setBillingInfo(billing);
        }

        Booking savedBooking = saveBooking(booking);

        log.info("Booking {} successfully created.", savedBooking.getBookingId());

        BookingConfirmationResponse response = mapToResponse(savedBooking, roomType.getRoomTypeName(), guestsNode,
                calculatedPrice);

        try {
            bookingEmailService.sendBookingConfirmation(buildBookingEmailPayload(savedBooking, roomType.getRoomTypeName(),
                    guestsNode));
        } catch (RuntimeException ex) {
            log.warn("Booking {} was created successfully, but confirmation email failed for recipient {}: {}",
                    savedBooking.getBookingId(),
                    savedBooking.getTravellerInfo() != null ? savedBooking.getTravellerInfo().getEmail() : "unknown",
                    ex.getMessage(),
                    ex);
        }

        return response;
    }

    /**
     * Retrieves a confirmed booking for the specified tenant with pricing
     * reconstruction.
     *
     * <p>
     * This operation validates the tenant and booking ownership, retrieves the
     * booking details
     * from the database, reconstructs the pricing breakdown from stored values, and
     * returns the complete
     * booking confirmation response with room images and formatted details.
     * </p>
     * <p>
     * Note: This method is read-only and does not require a transaction since it
     * only performs read operations.
     * 
     * @param tenantId  the unique identifier of the tenant requesting the booking
     *                  (will be validated
     *                  as a valid UUID and checked for existence)
     * @param bookingId the unique reference identifier of the booking to retrieve
     *                  (will be validated
     *                  as a valid UUID format)
     * @return the booking confirmation response with pricing, traveler, billing,
     *         payment, and
     *         room image details for the requested booking
     * @throws InvalidBookingRequestException if the tenantId or bookingId cannot be
     *                                        parsed as valid UUIDs
     * @throws BookingNotFoundException       if no booking exists with the provided
     *                                        bookingId for the specified tenant
     * @see BookingConfirmationResponse
     */
    @Transactional(readOnly = true)
    public BookingConfirmationResponse getBooking(String tenantId, String bookingId) {
        UUID tenantUuid = validationService.parseUuid(tenantId, "tenantId");
        UUID bookingUuid = validationService.parseUuid(bookingId, "bookingId");

        validationService.validateTenantExists(tenantUuid);

        Booking booking = bookingRepository.findByBookingIdAndProperty_Tenant_TenantId(bookingUuid, tenantUuid)
                .orElseThrow(() -> new com.example.ibe.exception.BookingNotFoundException(
                        "BOOKING_NOT_FOUND: Could not find booking with reference " + bookingId));

        BookingPriceCalculation calc = pricingService.reconstructPricing(
                booking.getTotalPrice(),
                booking.getTaxAmount(),
                booking.getFinalAmount(),
                booking.getProperty().getDuePercentage());

        return mapToResponse(booking, booking.getRoomType().getRoomTypeName(), booking.getGuestsJson(), calc);
    }

    /**
     * Recovery handler for booking creation after all retry attempts fail due to
     * concurrent booking contention.
     *
     * <p>
     * Called by Spring's {@code @Recover} mechanism after the maximum retry
     * attempts are exhausted.
     * Logs the failure and throws a user-friendly exception indicating that the
     * rooms were booked by another user.
     *
     * @param ex      the concurrent booking contention exception that triggered
     *                retries
     * @param request the original booking request that failed
     * @return never returns; always throws RoomNotAvailableException
     * @throws RoomNotAvailableException always, with a message instructing the user
     *                                   to refresh availability
     */
    @Recover
    public BookingConfirmationResponse recoverFromContention(
            ConcurrentBookingContentionException ex,
            CreateBookingRequest request) {
        log.warn("Room lock contention persisted after retries for roomTypeId={}", request.getRoomTypeId(), ex);
        throw new RoomNotAvailableException(
                "ROOM_NOT_AVAILABLE: The selected rooms were just booked by another user. Please refresh availability and try again.");
    }

    /**
     * Locks specific physical rooms for the booking using pessimistic database
     * locking (FOR UPDATE).
     *
     * <p>
     * Retrieves and locks exactly {@code roomCount} available rooms that have no
     * overlapping
     * assignments during the specified date range. If fewer rooms can be locked
     * than requested,
     * throws {@code ConcurrentBookingContentionException} to trigger retry logic.
     *
     * @param roomTypeId   the room type to lock rooms from
     * @param checkInDate  the stay start date
     * @param checkOutDate the stay end date
     * @param roomCount    the number of rooms to lock
     * @return list of locked room UUIDs
     * @throws ConcurrentBookingContentionException if fewer than {@code roomCount}
     *                                              rooms could be locked
     * @throws BookingAvailabilityException         if database operations fail
     */
    private List<UUID> lockAvailableRooms(
            UUID roomTypeId,
            java.time.LocalDate checkInDate,
            java.time.LocalDate checkOutDate,
            int roomCount) {
        try {
            List<UUID> lockedRoomIds = roomRepository.findAndLockAvailablePhysicalRooms(
                    roomTypeId,
                    checkInDate,
                    checkOutDate,
                    roomCount);

            if (lockedRoomIds.size() < roomCount) {
                log.warn(
                        "Not enough physical rooms could be locked for RoomType {}. Another user likely just booked them.",
                        roomTypeId);
                throw new ConcurrentBookingContentionException("Concurrent booking contention while assigning rooms");
            }

            return lockedRoomIds;
        } catch (ConcurrentBookingContentionException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new BookingAvailabilityException(
                    "BOOKING_AVAILABILITY_ERROR: Unable to reserve rooms right now. Please try again.",
                    ex);
        }
    }

    /**
     * Persists the booking entity and all associated relationships to the database.
     *
     * <p>
     * Attempts to save the booking with related room assignments, traveler info,
     * billing info, and payment info.
     * Handles specific database constraint violations (overlapping room
     * assignments) with appropriate exceptions.
     *
     * @param booking the booking entity to persist (must not be null)
     * @return the persisted booking with database-generated IDs
     * @throws InvalidBookingRequestException if booking is null
     * @throws RoomNotAvailableException      if a room assignment constraint
     *                                        violation occurs (overlapping dates)
     * @throws BookingPersistenceException    for other database errors
     */
    private Booking saveBooking(Booking booking) {
        if (booking == null) {
            throw new InvalidBookingRequestException(
                    "INVALID_BOOKING_REQUEST: Booking information is required to save.");
        }
        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            if (isRoomAssignmentConflict(ex)) {
                throw new RoomNotAvailableException(
                        "ROOM_NOT_AVAILABLE: The selected rooms were just booked by another user. Please refresh availability and try again.");
            }
            throw new BookingPersistenceException(
                    "BOOKING_PERSISTENCE_ERROR: Unable to complete the booking right now. Please try again.",
                    ex);
        } catch (DataAccessException ex) {
            throw new BookingPersistenceException(
                    "BOOKING_PERSISTENCE_ERROR: Unable to complete the booking right now. Please try again.",
                    ex);
        }
    }

    /**
     * Determines if a database constraint violation is due to overlapping room
     * assignments.
     *
     * <p>
     * Examines the exception message to detect violations of the
     * {@code no_overlap_assignments} constraint,
     * which indicates an attempt to assign a room to overlapping date ranges within
     * a single booking.
     *
     * @param ex the database integrity violation exception
     * @return true if the violation is due to overlapping room assignments; false
     *         otherwise
     */
    private boolean isRoomAssignmentConflict(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.contains("no_overlap_assignments");
    }

    /**
     * Converts a Booking entity to a BookingConfirmationResponse DTO with all
     * formatted details.
     *
     * <p>
     * Fetches room images, builds traveler/billing/payment/pricing DTOs, and
     * assembles the complete
     * response with booking confirmation details ready for API response
     * serialization.
     *
     * @param booking      the booking entity containing all reservation data
     * @param roomTypeName the display name of the booked room type
     * @param guests       the guest information as a JSON node
     * @param pricing      the calculated pricing breakdown
     * @return the complete booking confirmation response with all details and
     *         images
     */
    private BookingConfirmationResponse mapToResponse(Booking booking, String roomTypeName, JsonNode guests,
            BookingPriceCalculation pricing) {
        List<String> imageUrls = roomTypeImageRepository
                .findByRoomType_RoomTypeId(booking.getRoomType().getRoomTypeId()).stream()
                .map(RoomTypeImage::getImageUrl)
                .toList();

        TravelerResponseDTO travelerRes = TravelerResponseDTO.builder()
                .firstName(booking.getTravellerInfo().getFirstName())
                .lastName(booking.getTravellerInfo().getLastName())
                .email(booking.getTravellerInfo().getEmail())
                .phone(booking.getTravellerInfo().getPhoneNumber())
                .build();

        BillingResponseDTO billingRes = null;
        if (booking.getBillingInfo() != null) {
            billingRes = BillingResponseDTO.builder()
                    .firstName(booking.getBillingInfo().getFirstName())
                    .lastName(booking.getBillingInfo().getLastName())
                    .address1(booking.getBillingInfo().getMailingAddress1())
                    .address2(booking.getBillingInfo().getMailingAddress2())
                    .city(booking.getBillingInfo().getCity())
                    .state(booking.getBillingInfo().getState())
                    .country(booking.getBillingInfo().getCountry())
                    .zip(booking.getBillingInfo().getZipCode())
                    .email(booking.getBillingInfo().getEmail())
                    .phone(booking.getBillingInfo().getPhoneNumber())
                    .build();
        }

        PaymentResponseDTO paymentRes = PaymentResponseDTO.builder()
                .paymentId(booking.getPaymentInfo().getPaymentId().toString())
                .status(booking.getPaymentInfo().getPaymentStatus().name())
                .paymentMethod(booking.getPaymentInfo().getPaymentMethod().name())
                .lastFourDigits(booking.getPaymentInfo().getLastFourDigits())
                .build();

        PricingResponseDTO pricingRes = PricingResponseDTO.builder()
                .subtotal(pricing.subtotal())
                .taxesAndCharges(pricing.taxesAndCharges())
                .dueAtResort(pricing.dueAtResort())
                .dueNow(pricing.dueNow())
                .currency(pricing.currency())
                .build();

        return BookingConfirmationResponse.builder()
                .bookingId(booking.getBookingId().toString())
                .status(booking.getBookingStatus().name())
                .roomTypeName(roomTypeName)
                .imageUrls(imageUrls)
                .guests(guests)
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .traveler(travelerRes)
                .billing(billingRes)
                .payment(paymentRes)
                .pricing(pricingRes)
                .build();
    }

    private BookingEmailPayload buildBookingEmailPayload(Booking booking, String roomTypeName, JsonNode guestsNode) {
        return new BookingEmailPayload(
                booking.getTravellerInfo().getEmail(),
                booking.getBookingId().toString(),
                booking.getProperty().getPropertyName(),
                roomTypeName,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                buildGuestSummary(guestsNode, booking.getNoOfGuests()));
    }

    private String buildGuestSummary(JsonNode guestsNode, Integer totalGuests) {
        String breakdown = buildGuestBreakdown(guestsNode);
        if (breakdown != null) {
            return breakdown;
        }

        int guestCount = totalGuests != null ? Math.max(totalGuests, 0) : 0;
        return guestCount == 1 ? "1 guest" : guestCount + " guests";
    }

    private String buildGuestBreakdown(JsonNode guestsNode) {
        if (guestsNode == null || !guestsNode.isObject()) {
            return null;
        }

        java.util.List<String> parts = new java.util.ArrayList<>();
        addGuestPart(parts, guestsNode, "adult", "adults", "adult");
        addGuestPart(parts, guestsNode, "child", "children", "child", "kids", "kid");
        addGuestPart(parts, guestsNode, "infant", "infants", "infant");
        addGuestPart(parts, guestsNode, "teen", "teens", "teen");

        if (parts.isEmpty()) {
            return null;
        }
        return String.join(", ", parts);
    }

    private void addGuestPart(java.util.List<String> parts, JsonNode guestsNode, String singularLabel, String primaryKey,
            String... alternateKeys) {
        Integer count = firstPositiveCount(guestsNode, primaryKey, alternateKeys);
        if (count == null) {
            return;
        }
        parts.add(count + " " + (count == 1 ? singularLabel : primaryKey));
    }

    private Integer firstPositiveCount(JsonNode guestsNode, String primaryKey, String... alternateKeys) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        keys.add(primaryKey);
        keys.addAll(java.util.List.of(alternateKeys));

        for (String key : keys) {
            JsonNode value = guestsNode.get(key);
            if (value != null && value.isNumber() && value.asInt() > 0) {
                return value.asInt();
            }
        }
        return null;
    }
}
