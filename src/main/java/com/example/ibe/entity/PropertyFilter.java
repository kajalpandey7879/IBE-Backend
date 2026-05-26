package com.example.ibe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import com.example.ibe.dto.enums.FilterType;

@Entity
@Table(name = "property_filters", indexes = {
    @Index(name = "idx_property_filters_property", columnList = "property_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_property_filter_name", columnNames = {"property_id", "filter_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyFilter {

    @Id
    @GeneratedValue
    @Column(name = "filter_id", nullable = false, updatable = false)
    private UUID filterId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "filter_name", nullable = false, length = 100)
    private String filterName;

    @Column(name = "filter_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FilterType filterType;
    
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private String configJson = "{}";
}