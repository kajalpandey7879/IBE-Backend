package com.example.ibe.repository;

import com.example.ibe.entity.TravellerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TravellerInfoRepository extends JpaRepository<TravellerInfo, UUID> {
    Optional<TravellerInfo> findByEmail(String email);
}
