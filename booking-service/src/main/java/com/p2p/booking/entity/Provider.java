package com.p2p.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "providers", indexes = {
    @Index(name = "idx_provider_geohash", columnList = "geohash"),
    @Index(name = "idx_provider_type_active", columnList = "providerType, isActive"),
    @Index(name = "idx_provider_phone", columnList = "phoneNumber")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;
    
    @NotBlank
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(length = 200)
    private String email;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;
    
    @NotBlank
    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;
    
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;
    
    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    // Location fields for GeoHash indexing
    @Column(name = "base_latitude", precision = 10, scale = 8)
    private BigDecimal baseLatitude;
    
    @Column(name = "base_longitude", precision = 11, scale = 8)
    private BigDecimal baseLongitude;
    
    @Column(name = "geohash", length = 20)
    private String geoHash;
    
    @Column(name = "service_radius_km", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal serviceRadiusKm = new BigDecimal("50.0"); // 50km default service radius
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();
    
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Route> routes = new ArrayList<>();
    
    public enum ProviderType {
        MATATU, TAXI, MOTORBIKE, BRT, EV
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}