package com.example.ibe.repository;

import com.example.ibe.entity.RoomTypeImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, UUID> {
    List<RoomTypeImage> findByRoomType_RoomTypeId(UUID roomTypeId);
}
