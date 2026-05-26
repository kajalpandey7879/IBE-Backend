package com.example.ibe.repository;

import com.example.ibe.dto.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookingAvailabilityRepositoryIntegrationTest {

    private static final UUID OCEAN_PROPERTY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CITY_PROPERTY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID DELUXE_ROOM_TYPE_ID = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    private static final UUID CITY_STANDARD_ROOM_TYPE_ID = UUID.fromString("d4444444-4444-4444-4444-444444444444");
    private static final UUID EXECUTIVE_ROOM_TYPE_ID = UUID.fromString("dc000001-0000-0000-0000-000000000001");
    private static final UUID TRAVELLER_ID = UUID.fromString("70000000-0000-0000-0000-000000000003");

    @Autowired
    private BookingAvailabilityRepository bookingAvailabilityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findMinPricePerDayReturnsSeededMinimumPriceForOceanProperty() {
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(4);

        List<DailyPriceProjection> results = bookingAvailabilityRepository.findMinPricePerDay(
                OCEAN_PROPERTY_ID,
                startDate,
                endDate);

        assertEquals(3, results.size());
        assertEquals(startDate, results.get(0).getDate());
        assertEquals(new BigDecimal("200.00"), results.get(0).getMinPrice());
        assertEquals(new BigDecimal("200.00"), results.get(1).getMinPrice());
        assertEquals(new BigDecimal("200.00"), results.get(2).getMinPrice());
    }

    @Test
    void findMinPriceForFullStayReturnsSeededMinimumPriceForOceanProperty() {
        BigDecimal minPrice = bookingAvailabilityRepository.findMinPriceForFullStay(
                OCEAN_PROPERTY_ID,
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(4));

        assertEquals(new BigDecimal("200.00"), minPrice);
    }

    @Test
    void findAvailableRoomTypeIdsHonorsCapacityAcrossEntireStay() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);
        long totalDays = 3L;

        List<UUID> availableForNineRooms = bookingAvailabilityRepository.findAvailableRoomTypeIds(
                OCEAN_PROPERTY_ID,
                checkIn,
                checkOut,
                9,
                totalDays);
        List<UUID> availableForTenRooms = bookingAvailabilityRepository.findAvailableRoomTypeIds(
                OCEAN_PROPERTY_ID,
                checkIn,
                checkOut,
                10,
                totalDays);

        assertTrue(availableForNineRooms.contains(DELUXE_ROOM_TYPE_ID));
        assertFalse(availableForTenRooms.contains(DELUXE_ROOM_TYPE_ID));
        assertTrue(availableForTenRooms.isEmpty());
    }

    @Test
    void isRoomTypeAvailableReflectsOverlappingConfirmedBookingLoad() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);
        long totalDays = 3L;

        assertTrue(bookingAvailabilityRepository.isRoomTypeAvailable(
                OCEAN_PROPERTY_ID,
                DELUXE_ROOM_TYPE_ID,
                checkIn,
                checkOut,
                9,
                totalDays));
        assertFalse(bookingAvailabilityRepository.isRoomTypeAvailable(
                OCEAN_PROPERTY_ID,
                DELUXE_ROOM_TYPE_ID,
                checkIn,
                checkOut,
                10,
                totalDays));
    }

    @Test
    void activeLocksCountButExpiredLocksAreIgnored() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(12);
        long totalDays = 2L;

        insertBooking(
                CITY_PROPERTY_ID,
                CITY_STANDARD_ROOM_TYPE_ID,
                12,
                checkIn,
                checkOut,
                BookingStatus.LOCKED,
                LocalDateTime.now().plusMinutes(15));

        insertBooking(
                CITY_PROPERTY_ID,
                EXECUTIVE_ROOM_TYPE_ID,
                8,
                checkIn,
                checkOut,
                BookingStatus.LOCKED,
                LocalDateTime.now().minusMinutes(15));

        List<DailyPriceProjection> minPrices = bookingAvailabilityRepository.findMinPricePerDay(
                CITY_PROPERTY_ID,
                checkIn,
                checkOut.minusDays(1));

        List<UUID> availableRoomTypeIds = bookingAvailabilityRepository.findAvailableRoomTypeIds(
                CITY_PROPERTY_ID,
                checkIn,
                checkOut,
                1,
                totalDays);

        assertEquals(2, minPrices.size());
        assertEquals(new BigDecimal("200.00"), minPrices.get(0).getMinPrice());
        assertEquals(new BigDecimal("200.00"), minPrices.get(1).getMinPrice());
        assertFalse(availableRoomTypeIds.contains(CITY_STANDARD_ROOM_TYPE_ID));
        assertTrue(availableRoomTypeIds.contains(EXECUTIVE_ROOM_TYPE_ID));
    }

    private UUID insertBooking(
            UUID propertyId,
            UUID roomTypeId,
            int roomCount,
            LocalDate checkIn,
            LocalDate checkOut,
            BookingStatus status,
            LocalDateTime expiresAt) {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO payment_info (
                    payment_id,
                    payment_status,
                    payment_method,
                    last_four_digits,
                    created_at,
                    updated_at,
                    paid_at,
                    failed_at
                ) VALUES (?, 'PENDING', 'CREDIT_CARD', '9999', ?, ?, NULL, NULL)
                """,
                paymentId,
                LocalDateTime.now(),
                LocalDateTime.now());
        jdbcTemplate.update("""
                INSERT INTO bookings (
                    booking_id,
                    traveller_id,
                    property_id,
                    room_type_id,
                    check_in_date,
                    check_out_date,
                    no_of_rooms,
                    no_of_guests,
                    guests_json,
                    total_price,
                    tax_amount,
                    final_amount,
                    booking_status,
                    billing_info_id,
                    payment_id,
                    locked_at,
                    confirmed_at,
                    expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(NULL AS jsonb), ?, ?, ?, ?, NULL, ?, ?, NULL, ?)
                """,
                bookingId,
                TRAVELLER_ID,
                propertyId,
                roomTypeId,
                checkIn,
                checkOut,
                roomCount,
                2,
                new BigDecimal("999.00"),
                new BigDecimal("99.00"),
                new BigDecimal("1098.00"),
                status.name(),
                paymentId,
                LocalDateTime.now(),
                expiresAt);
        return bookingId;
    }
}
