package com.example.ibe.repository;

import com.example.ibe.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {
    Optional<PromoCode> findByProperty_PropertyIdAndPromoCode(UUID propertyId, String promoCode);
    Optional<PromoCode> findByProperty_PropertyIdAndPromoName(UUID propertyId, String promoName);
}
