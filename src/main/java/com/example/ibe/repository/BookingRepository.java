package com.example.ibe.repository;

import com.example.ibe.entity.Booking;
import com.example.ibe.dto.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByProperty_PropertyId(UUID propertyId);

    List<Booking> findByRoomType_RoomTypeId(UUID roomTypeId);

    List<Booking> findByTravellerInfo_TravellerId(UUID travellerId);

    List<Booking> findByBookingStatus(BookingStatus bookingStatus);

    List<Booking> findByRoomType_RoomTypeIdAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            UUID roomTypeId,
            LocalDate checkOutDate,
            LocalDate checkInDate);

    Optional<Booking> findByBookingIdAndProperty_Tenant_TenantId(UUID bookingId, UUID tenantId);
}
