package com.joe_bor.svt_api.models.event;

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
@Table(name = "event_choice")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventChoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column(name = "label", nullable = false, length = 64)
    private String label;

    @Column(name = "cash_effect", nullable = false)
    private int cashEffect;

    @Column(name = "customers_effect", nullable = false)
    private int customersEffect;

    @Column(name = "morale_effect", nullable = false)
    private int moraleEffect;

    @Column(name = "coffee_effect", nullable = false)
    private int coffeeEffect;

    @Column(name = "bugs_effect", nullable = false)
    private int bugsEffect;

    @Enumerated(EnumType.STRING)
    @Column(name = "special_effect", length = 20)
    private SpecialEffectType specialEffect;
}
