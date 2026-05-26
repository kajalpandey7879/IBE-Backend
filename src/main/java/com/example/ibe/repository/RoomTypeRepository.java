package com.example.ibe.repository;

import com.example.ibe.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    List<RoomType> findByProperty_PropertyId(UUID propertyId);

    Optional<RoomType> findByRoomTypeIdAndProperty_PropertyId(UUID roomTypeId, UUID propertyId);

    @Query("""
            SELECT rt
            FROM RoomType rt
            WHERE rt.property.propertyId = :propertyId
            AND rt.roomTypeId IN :roomTypeIds
            AND (rt.maxOccupancy * :requiredRooms) >= :requestedGuests
            """)
    List<RoomType> findAvailableRoomTypesByIds(
            @Param("propertyId") UUID propertyId,
            @Param("roomTypeIds") Collection<UUID> roomTypeIds,
            @Param("requiredRooms") Integer requiredRooms,
            @Param("requestedGuests") Integer requestedGuests);
}
