package com.joe_bor.svt_api.services.action;

import com.joe_bor.svt_api.common.GameConflictException;
import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.controllers.game.dto.AvailableNextLocationDto;
import com.joe_bor.svt_api.models.location.DetourBonusStat;
import com.joe_bor.svt_api.models.location.LocationEntity;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationProgressionService {

    private final LocationRepository locationRepository;
    private final GameBalanceProperties balance;

    @Transactional
    public TravelOutcome travel(
            GameSessionEntity session,
            long destinationLocationId,
            List<AvailableNextLocationDto> legalDestinations
    ) {
        LocationEntity destination = locationRepository.findById(destinationLocationId)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + destinationLocationId));

        if (legalDestinations.stream().noneMatch(legalDestination -> legalDestination.locationId() == destinationLocationId)) {
            throw new GameConflictException("Invalid travel destination from " + session.getCurrentLocation().getName());
        }

        session.setCash(session.getCash() - balance.actionCosts().travel().cashCost());
        session.setCoffee(session.getCoffee() - balance.actionCosts().travel().coffeeCost());
        session.setCurrentLocation(destination);

        // Detours resolve immediately on arrival instead of creating a separate follow-up turn effect.
        String detourBonusApplied = applyDetourBonus(session, destination);
        return new TravelOutcome(destination.getId(), detourBonusApplied);
    }

    // Applies the one-off reward attached to detour locations like Cupertino or Woodside.
    private String applyDetourBonus(GameSessionEntity session, LocationEntity destination) {
        if (destination.getDetourBonusStat() == null || destination.getDetourBonusValue() == null) {
            return null;
        }

        int bonusValue = destination.getDetourBonusValue();
        DetourBonusStat stat = destination.getDetourBonusStat();
        switch (stat) {
            case CASH -> session.setCash(session.getCash() + bonusValue);
            case CUSTOMERS -> session.setCustomers(session.getCustomers() + bonusValue);
            case MORALE -> session.setMorale(session.getMorale() + bonusValue);
            case COFFEE -> session.setCoffee(session.getCoffee() + bonusValue);
            case BUGS -> session.setBugs(session.getBugs() + bonusValue);
        }

        return stat.name() + (bonusValue >= 0 ? " +" : " ") + bonusValue;
    }

    public record TravelOutcome(
            long destinationLocationId,
            String detourBonusApplied
    ) {
    }
}
