package com.example.ibe.repository;

import com.example.ibe.entity.BillingInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BillingInfoRepository extends JpaRepository<BillingInfo, UUID> {
}
