package com.example.ibe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Amenity {

    @Id
    @GeneratedValue
    @Column(name = "amenity_id", nullable = false, updatable = false)
    private UUID amenityId;

    @Column(name = "amenity_name", nullable = false, unique = true, length = 120)
    private String amenityName;

    @ManyToMany(mappedBy = "amenities", fetch = FetchType.LAZY)
    private List<RoomType> roomTypes;
}
