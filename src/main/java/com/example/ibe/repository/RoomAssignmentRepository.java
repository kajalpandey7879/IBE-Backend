package com.example.ibe.repository;

import com.example.ibe.entity.RoomAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, UUID> {
    List<RoomAssignment> findByBooking_BookingId(UUID bookingId);

    List<RoomAssignment> findByRoom_RoomId(UUID roomId);

    List<RoomAssignment> findByRoom_RoomIdAndStartDateLessThanAndEndDateGreaterThan(
            UUID roomId,
            LocalDate endDate,
            LocalDate startDate);
}
