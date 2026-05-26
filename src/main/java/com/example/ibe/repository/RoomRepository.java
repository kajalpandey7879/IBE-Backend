package com.example.ibe.repository;

import com.example.ibe.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByRoomType_RoomTypeId(UUID roomTypeId);

    Optional<Room> findByRoomType_RoomTypeIdAndHotelRoomNo(UUID roomTypeId, String hotelRoomNo);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT r.room_id 
            FROM rooms r 
            WHERE r.room_type_id = :roomTypeId 
              AND r.room_id NOT IN (
                  SELECT ra.room_id 
                  FROM room_assignments ra 
                  WHERE ra.start_date < :checkOutDate 
                    AND ra.end_date > :checkInDate
              )
            LIMIT :limit 
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> findAndLockAvailablePhysicalRooms(
            @org.springframework.data.repository.query.Param("roomTypeId") UUID roomTypeId,
            @org.springframework.data.repository.query.Param("checkInDate") java.time.LocalDate checkInDate,
            @org.springframework.data.repository.query.Param("checkOutDate") java.time.LocalDate checkOutDate,
            @org.springframework.data.repository.query.Param("limit") int limit);
}
