package com.example.ibe.repository;

import com.example.ibe.entity.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, UUID> {
}
