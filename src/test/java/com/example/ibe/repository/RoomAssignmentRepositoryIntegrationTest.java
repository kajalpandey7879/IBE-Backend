package com.example.ibe.repository;

import com.example.ibe.dto.enums.BookingStatus;
import com.example.ibe.entity.Booking;
import com.example.ibe.entity.Room;
import com.example.ibe.entity.RoomAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RoomAssignmentRepositoryIntegrationTest {

    private static final UUID OCEAN_PROPERTY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DELUXE_ROOM_TYPE_ID = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    private static final UUID TRAVELLER_ID = UUID.fromString("70000000-0000-0000-0000-000000000003");
    private static final UUID SEEDED_BOOKING_ID = UUID.fromString("72000000-0000-0000-0000-000000000001");

    @Autowired
    private RoomAssignmentRepository roomAssignmentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void overlappingAssignmentForSameRoomIsRejected() {
        Room room = roomRepository.findByRoomType_RoomTypeId(DELUXE_ROOM_TYPE_ID).stream()
                .findFirst()
                .orElseThrow();

        UUID bookingId = insertConfirmedBooking(
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(6));
        Booking booking = bookingRepository.getReferenceById(bookingId);

        RoomAssignment assignment = new RoomAssignment();
        assignment.setBooking(booking);
        assignment.setRoom(room);
        assignment.setStartDate(LocalDate.now().plusDays(3));
        assignment.setEndDate(LocalDate.now().plusDays(4));

        assertThrows(DataIntegrityViolationException.class, () -> roomAssignmentRepository.saveAndFlush(assignment));
    }

    @Test
    void nonOverlappingAssignmentForSameRoomIsAllowed() {
        Room room = roomRepository.findByRoomType_RoomTypeId(DELUXE_ROOM_TYPE_ID).stream()
                .findFirst()
                .orElseThrow();

        UUID bookingId = insertConfirmedBooking(
                LocalDate.now().plusDays(6),
                LocalDate.now().plusDays(8));
        Booking booking = bookingRepository.getReferenceById(bookingId);

        RoomAssignment assignment = new RoomAssignment();
        assignment.setBooking(booking);
        assignment.setRoom(room);
        assignment.setStartDate(LocalDate.now().plusDays(6));
        assignment.setEndDate(LocalDate.now().plusDays(8));

        assertDoesNotThrow(() -> roomAssignmentRepository.saveAndFlush(assignment));
    }

    @Test
    void seededAssignmentCanBeReadBackByBookingId() {
        List<RoomAssignment> assignments = roomAssignmentRepository.findByBooking_BookingId(SEEDED_BOOKING_ID);

        org.junit.jupiter.api.Assertions.assertEquals(1, assignments.size());
    }

    private UUID insertConfirmedBooking(LocalDate checkIn, LocalDate checkOut) {
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
                ) VALUES (?, 'PAID', 'CREDIT_CARD', '4242', ?, ?, ?, NULL)
                """,
                paymentId,
                LocalDateTime.now(),
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
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(NULL AS jsonb), ?, ?, ?, ?, NULL, ?, NULL, ?, NULL)
                """,
                bookingId,
                TRAVELLER_ID,
                OCEAN_PROPERTY_ID,
                DELUXE_ROOM_TYPE_ID,
                checkIn,
                checkOut,
                1,
                2,
                new BigDecimal("450.00"),
                new BigDecimal("54.00"),
                new BigDecimal("504.00"),
                BookingStatus.CONFIRMED.name(),
                paymentId,
                LocalDateTime.now());
        return bookingId;
    }
}
