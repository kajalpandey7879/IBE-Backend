package com.example.ibe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.ibe.dto.enums.MealPlan;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {

    @Id
    @GeneratedValue
    @Column(name = "room_type_id", nullable = false, updatable = false)
    private UUID roomTypeId;

    @Column(name = "room_type_name", nullable = false)
    private String roomTypeName;

    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "area", nullable = false)
    private Integer area;

    @Column(name = "description")
    private String description;

    @Column(name = "bed_types", columnDefinition = "jsonb", nullable = false)
    private String bedTypes = "{}";

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_plan")
    private MealPlan mealPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "room_type_amenities", joinColumns = @JoinColumn(name = "room_type_id"), inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private List<Amenity> amenities;
}