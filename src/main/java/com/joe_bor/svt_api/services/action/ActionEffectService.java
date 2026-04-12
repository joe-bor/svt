package com.joe_bor.svt_api.services.action;

import com.joe_bor.svt_api.common.DomainValidationException;
import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.controllers.action.dto.SubmitActionRequest;
import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.controllers.game.dto.AvailableNextLocationDto;
import com.joe_bor.svt_api.models.gameplay.ActionType;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.services.crypto.CryptoSettlementService;
import com.joe_bor.svt_api.services.weather.WeatherModifierService;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionEffectService {

    private static final TurnResolutionSummaryDto.WeatherSurcharges ZERO_WEATHER_SURCHARGES =
            new TurnResolutionSummaryDto.WeatherSurcharges(0);

    private final GameBalanceProperties balance;
    private final LocationProgressionService locationProgressionService;
    private final CryptoSettlementService cryptoSettlementService;
    private final WeatherModifierService weatherModifierService;

    @Transactional
    public TurnResolutionSummaryDto.ActionResolutionDetail applyAction(
            GameSessionEntity session,
            SubmitActionRequest.ActionPayload action,
            List<AvailableNextLocationDto> legalDestinations,
            WeatherSnapshot weather
    ) {
        // 1. Capture the visible pre-action state so the response can report only this action's delta.
        VisibleStatsSnapshot before = VisibleStatsSnapshot.capture(session);
        Long destinationLocationId = null;
        String detourBonusApplied = null;
        List<String> notes = List.of();
        TurnResolutionSummaryDto.WeatherSurcharges weatherSurcharges = ZERO_WEATHER_SURCHARGES;

        // 2. Apply the chosen action's game rule.
        switch (action.type()) {
            case TRAVEL -> {
                var surcharge = weatherModifierService.computeTravelSurcharge(weather.bucket());
                if (surcharge.coffeeAdded() != 0) {
                    session.setCoffee(session.getCoffee() - surcharge.coffeeAdded());
                }
                LocationProgressionService.TravelOutcome outcome = locationProgressionService.travel(
                        session,
                        action.destinationLocationId(),
                        legalDestinations
                );
                WeatherModifierService.StormyTravelPenalty stormyTravelPenalty =
                        weatherModifierService.computeStormyTravelPenalty(weather.bucket());
                if (stormyTravelPenalty.cash() != 0) {
                    session.setCash(session.getCash() + stormyTravelPenalty.cash());
                }
                if (stormyTravelPenalty.morale() != 0) {
                    session.setMorale(session.getMorale() + stormyTravelPenalty.morale());
                }
                destinationLocationId = outcome.destinationLocationId();
                detourBonusApplied = outcome.detourBonusApplied();
                weatherSurcharges = new TurnResolutionSummaryDto.WeatherSurcharges(surcharge.coffeeAdded());
            }
            case REST -> {
                session.setMorale(session.getMorale() + balance.actionCosts().rest().moraleGain());
                session.setCoffee(session.getCoffee() + balance.actionCosts().rest().coffeeGain());
            }
            case WORK_ON_PRODUCT -> {
                session.setMorale(session.getMorale() - balance.actionCosts().workOnProduct().moraleCost());
                session.setCoffee(session.getCoffee() - balance.actionCosts().workOnProduct().coffeeCost());
                session.setBugs(session.getBugs() - balance.actionCosts().workOnProduct().bugsReduced());
            }
            case MARKETING -> {
                session.setCash(session.getCash() - balance.actionCosts().marketing().cashCost());
                session.setCustomers(session.getCustomers() + balance.actionCosts().marketing().customersGained());
            }
            case PITCH_VCS -> {
                session.setMorale(session.getMorale() - balance.actionCosts().pitchVcs().moraleCost());
                session.setCoffee(session.getCoffee() - balance.actionCosts().pitchVcs().coffeeCost());
                int cashGain = balance.actionCosts().pitchVcs().cashGained();
                if (session.isLinkedinBonusActive()) {
                    // The LinkedIn recruiter event turns the next VC pitch into a bigger payday once.
                    cashGain += balance.actionCosts().pitchVcs().linkedinBonusCash();
                    session.setLinkedinBonusActive(false);
                    notes = List.of("LinkedIn bonus applied");
                }
                session.setCash(session.getCash() + cashGain);
                session.setCustomers(session.getCustomers() + balance.actionCosts().pitchVcs().customersGained());
            }
            case BUY_SUPPLIES -> {
                session.setCash(session.getCash() - balance.actionCosts().buySupplies().cashCost());
                session.setCoffee(session.getCoffee() + balance.actionCosts().buySupplies().coffeeGained());
            }
            case INVEST_CRYPTO -> {
                Integer amount = action.amount();
                if (amount == null) {
                    throw new DomainValidationException("amount is required for INVEST_CRYPTO");
                }
                if (amount > session.getCash()) {
                    throw new DomainValidationException("Invest amount exceeds current cash");
                }
                session.setCash(session.getCash() - amount);
                // Crypto pays out on a later turn; this action only withholds the principal and books the settlement.
                session.setPendingCryptoSettlement(cryptoSettlementService.settle(amount, session.getCurrentGameDate()));
                notes = List.of("Principal withheld; settlement will resolve next turn");
            }
            case SKIP -> {
                // No-op. Forced-rest turns still apply events and passive updates before this point.
            }
        }

        // 3. Normalize the resulting state and publish this action's contribution to lastResolution.
        clampSession(session);
        VisibleStatsSnapshot after = VisibleStatsSnapshot.capture(session);

        return new TurnResolutionSummaryDto.ActionResolutionDetail(
                action.type(),
                after.cash() - before.cash(),
                after.coffee() - before.coffee(),
                after.morale() - before.morale(),
                after.customers() - before.customers(),
                destinationLocationId,
                weatherSurcharges,
                detourBonusApplied,
                notes
        );
    }

    // Action effects can push values outside their legal bounds, so normalize before building deltas.
    private static void clampSession(GameSessionEntity session) {
        session.setMorale(Math.max(0, Math.min(100, session.getMorale())));
        session.setCoffee(Math.max(0, session.getCoffee()));
        session.setCustomers(Math.max(0, session.getCustomers()));
        session.setBugs(Math.max(0, session.getBugs()));
    }

    private record VisibleStatsSnapshot(
            int cash,
            int customers,
            int morale,
            int coffee
    ) {
        // Snapshots the visible stats so the action response can report only this action's contribution.
        private static VisibleStatsSnapshot capture(GameSessionEntity session) {
            return new VisibleStatsSnapshot(
                    session.getCash(),
                    session.getCustomers(),
                    session.getMorale(),
                    session.getCoffee()
            );
        }
    }
}
