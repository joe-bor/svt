package com.joe_bor.svt_api.models.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 32)
    private String name;

    @Column(name = "description", nullable = false, length = 128)
    private String description;

    @Column(name = "route_order")
    private Short routeOrder;

    @Column(name = "is_detour", nullable = false)
    private boolean detour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branches_from_id")
    private LocationEntity branchesFrom;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "detour_bonus_stat", length = 16)
    private DetourBonusStat detourBonusStat;

    @Column(name = "detour_bonus_value")
    private Integer detourBonusValue;
}
