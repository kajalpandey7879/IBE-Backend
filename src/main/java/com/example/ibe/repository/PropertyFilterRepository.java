package com.example.ibe.repository;

import com.example.ibe.entity.PropertyFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyFilterRepository extends JpaRepository<PropertyFilter, UUID> {

    List<PropertyFilter> findByProperty_PropertyIdOrderByFilterNameAsc(UUID propertyId);
}