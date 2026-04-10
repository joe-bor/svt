package com.joe_bor.svt_api.models.session;

import com.joe_bor.svt_api.models.location.LocationEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game_session")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSessionEntity {

    public static GameSessionEntity create() {
        return new GameSessionEntity();
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GameSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_end_reason", length = 24)
    private GameEndReason gameEndReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id", nullable = false)
    private LocationEntity currentLocation;

    @Column(name = "game_start_date", nullable = false)
    private LocalDate gameStartDate;

    @Column(name = "current_game_date", nullable = false)
    private LocalDate currentGameDate;

    @Column(name = "cash", nullable = false)
    private int cash;

    @Column(name = "customers", nullable = false)
    private int customers;

    @Column(name = "morale", nullable = false)
    private int morale;

    @Column(name = "coffee", nullable = false)
    private int coffee;

    @Column(name = "bugs", nullable = false)
    private int bugs;

    @Column(name = "is_mutiny_ready", nullable = false)
    private boolean mutinyReady;

    @Column(name = "is_burnout_ready", nullable = false)
    private boolean burnoutReady;

    @Column(name = "is_linkedin_bonus_active", nullable = false)
    private boolean linkedinBonusActive;

    @Column(name = "pending_crypto_settlement")
    private Integer pendingCryptoSettlement;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "game_session_pending_events",
            joinColumns = @JoinColumn(name = "game_session_id")
    )
    @Column(name = "event_id")
    private Set<Long> pendingEventIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "game_session_fired_location_events",
            joinColumns = @JoinColumn(name = "game_session_id")
    )
    @Column(name = "event_id")
    private Set<Long> firedLocationEventIds = new HashSet<>();
}
