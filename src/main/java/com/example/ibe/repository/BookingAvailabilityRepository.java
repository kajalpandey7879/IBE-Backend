package com.example.ibe.repository;

import com.example.ibe.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingAvailabilityRepository extends JpaRepository<Booking, UUID> {

    @Query(value = """
            WITH date_range AS (
                SELECT d::date AS occupancy_date
                FROM generate_series(
                    CAST(:startDate AS TIMESTAMP),
                    CAST(:endDate AS TIMESTAMP),
                    INTERVAL '1 day'
                ) d
            ),
            active_bookings AS (
                SELECT
                    b.room_type_id,
                    b.check_in_date,
                    b.check_out_date,
                    b.no_of_rooms
                FROM bookings b
                WHERE b.property_id = :propertyId
                  AND (b.booking_status = 'CONFIRMED' OR (b.booking_status = 'LOCKED' AND b.expires_at > NOW()))
                  AND b.check_in_date <= :endDate
                  AND b.check_out_date > :startDate
            ),
            daily_availability AS (
                SELECT
                    dr.occupancy_date,
                    rt.room_type_id,
                    rt.base_price,
                    rt.total_rooms - COALESCE(SUM(ab.no_of_rooms), 0) AS available_rooms
                FROM date_range dr
                JOIN room_types rt ON rt.property_id = :propertyId
                LEFT JOIN active_bookings ab
                    ON ab.room_type_id = rt.room_type_id
                    AND dr.occupancy_date >= ab.check_in_date
                    AND dr.occupancy_date < ab.check_out_date
                GROUP BY dr.occupancy_date, rt.room_type_id, rt.base_price, rt.total_rooms
            )
            SELECT occupancy_date AS date, MIN(base_price) AS minPrice
            FROM daily_availability
            WHERE available_rooms > 0
            GROUP BY occupancy_date
            ORDER BY occupancy_date
            """, nativeQuery = true)
    List<DailyPriceProjection> findMinPricePerDay(
            @Param("propertyId") UUID propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = """
            WITH date_range AS (
                SELECT d::date AS occupancy_date
                FROM generate_series(
                    CAST(:startDate AS TIMESTAMP),
                    CAST(:endDate AS TIMESTAMP),
                    INTERVAL '1 day'
                ) d
            ),
            active_bookings AS (
                SELECT
                    b.room_type_id,
                    b.check_in_date,
                    b.check_out_date,
                    b.no_of_rooms
                FROM bookings b
                WHERE b.property_id = :propertyId
                  AND (b.booking_status = 'CONFIRMED' OR (b.booking_status = 'LOCKED' AND b.expires_at > NOW()))
                  AND b.check_in_date <= :endDate
                  AND b.check_out_date > :startDate
            ),
            daily_availability AS (
                SELECT
                    dr.occupancy_date,
                    rt.room_type_id,
                    rt.base_price,
                    rt.total_rooms - COALESCE(SUM(ab.no_of_rooms), 0) AS available_rooms
                FROM date_range dr
                JOIN room_types rt ON rt.property_id = :propertyId
                LEFT JOIN active_bookings ab
                    ON ab.room_type_id = rt.room_type_id
                    AND dr.occupancy_date >= ab.check_in_date
                    AND dr.occupancy_date < ab.check_out_date
                GROUP BY dr.occupancy_date, rt.room_type_id, rt.base_price, rt.total_rooms
            )
            SELECT MIN(base_price)
            FROM (
                SELECT room_type_id, base_price
                FROM daily_availability
                GROUP BY room_type_id, base_price
                HAVING MIN(available_rooms) > 0
            ) available_room_types
            """, nativeQuery = true)
    BigDecimal findMinPriceForFullStay(
            @Param("propertyId") UUID propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = """
            WITH date_range AS (
                SELECT d::date AS occupancy_date
                FROM generate_series(
                    CAST(:checkIn AS TIMESTAMP),
                    CAST(:checkOut AS TIMESTAMP) - INTERVAL '1 day',
                    INTERVAL '1 day'
                ) d
            ),
            active_bookings AS (
                SELECT
                    b.room_type_id,
                    b.check_in_date,
                    b.check_out_date,
                    b.no_of_rooms
                FROM bookings b
                WHERE b.property_id = :propertyId
                  AND (b.booking_status = 'CONFIRMED' OR (b.booking_status = 'LOCKED' AND b.expires_at > NOW()))
                  AND b.check_in_date < :checkOut
                  AND b.check_out_date > :checkIn
            ),
            daily_availability AS (
                SELECT
                    dr.occupancy_date,
                    rt.room_type_id,
                    rt.total_rooms - COALESCE(SUM(ab.no_of_rooms), 0) AS available_rooms
                FROM room_types rt
                CROSS JOIN date_range dr
                LEFT JOIN active_bookings ab
                    ON ab.room_type_id = rt.room_type_id
                    AND dr.occupancy_date >= ab.check_in_date
                    AND dr.occupancy_date < ab.check_out_date
                WHERE rt.property_id = :propertyId
                GROUP BY dr.occupancy_date, rt.room_type_id, rt.total_rooms
            )
            SELECT room_type_id
            FROM daily_availability
            GROUP BY room_type_id
            HAVING MIN(available_rooms) >= :requiredRooms
            """, nativeQuery = true)
    List<UUID> findAvailableRoomTypeIds(
            @Param("propertyId") UUID propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("requiredRooms") int requiredRooms,
            @Param("totalDays") long totalDays);

    @Query(value = """
            WITH date_range AS (
                SELECT d::date AS occupancy_date
                FROM generate_series(
                    CAST(:checkIn AS TIMESTAMP),
                    CAST(:checkOut AS TIMESTAMP) - INTERVAL '1 day',
                    INTERVAL '1 day'
                ) d
            ),
            room_type_capacity AS (
                SELECT rt.total_rooms
                FROM room_types rt
                WHERE rt.property_id = :propertyId
                  AND rt.room_type_id = :roomTypeId
            ),
            active_bookings AS (
                SELECT
                    b.check_in_date,
                    b.check_out_date,
                    b.no_of_rooms
                FROM bookings b
                WHERE b.property_id = :propertyId
                  AND b.room_type_id = :roomTypeId
                  AND (b.booking_status = 'CONFIRMED' OR (b.booking_status = 'LOCKED' AND b.expires_at > NOW()))
                  AND b.check_in_date < :checkOut
                  AND b.check_out_date > :checkIn
            ),
            daily_availability AS (
                SELECT
                    dr.occupancy_date,
                    rtc.total_rooms - COALESCE(SUM(ab.no_of_rooms), 0) AS available_rooms
                FROM date_range dr
                CROSS JOIN room_type_capacity rtc
                LEFT JOIN active_bookings ab
                    ON dr.occupancy_date >= ab.check_in_date
                    AND dr.occupancy_date < ab.check_out_date
                GROUP BY dr.occupancy_date, rtc.total_rooms
            )
            SELECT COALESCE(
                (
                    SELECT MIN(available_rooms) >= :requiredRooms
                    FROM daily_availability
                ),
                FALSE
            )
            """, nativeQuery = true)
    boolean isRoomTypeAvailable(
            @Param("propertyId") UUID propertyId,
            @Param("roomTypeId") UUID roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("requiredRooms") int requiredRooms,
            @Param("totalDays") long totalDays);
}
