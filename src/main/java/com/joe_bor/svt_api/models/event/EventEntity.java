package com.joe_bor.svt_api.models.event;

import com.joe_bor.svt_api.models.location.LocationEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 48)
    private String name;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    @Column(name = "has_choice", nullable = false)
    private boolean hasChoice;

    @Column(name = "auto_cash_effect", nullable = false)
    private int autoCashEffect;

    @Column(name = "auto_customers_effect", nullable = false)
    private int autoCustomersEffect;

    @Column(name = "auto_morale_effect", nullable = false)
    private int autoMoraleEffect;

    @Column(name = "auto_coffee_effect", nullable = false)
    private int autoCoffeeEffect;

    @Column(name = "auto_bugs_effect", nullable = false)
    private int autoBugsEffect;

    @Enumerated(EnumType.STRING)
    @Column(name = "special_effect", length = 20)
    private SpecialEffectType specialEffect;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<EventChoiceEntity> choices = new ArrayList<>();
}
