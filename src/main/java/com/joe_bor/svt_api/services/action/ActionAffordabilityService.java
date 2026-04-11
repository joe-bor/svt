package com.joe_bor.svt_api.services.action;

import com.joe_bor.svt_api.common.DomainValidationException;
import com.joe_bor.svt_api.common.GameConflictException;
import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.controllers.game.dto.AvailableActionDto;
import com.joe_bor.svt_api.models.gameplay.ActionType;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.services.route.RouteService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionAffordabilityService {

    private static final AvailableActionDto.WeatherSurcharge ZERO_WEATHER_SURCHARGE =
            new AvailableActionDto.WeatherSurcharge(0, 0, 0);

    private final GameBalanceProperties balance;
    private final RouteService routeService;

    public List<AvailableActionDto> computeAvailableActions(GameSessionEntity session, boolean hasLoseActionPending) {
        if (hasLoseActionPending) {
            return List.of(baseAction(ActionType.SKIP, 0, 0, 0, false, false, null, null));
        }

        List<AvailableActionDto> actions = new ArrayList<>();
        GameBalanceProperties.ActionCosts actionCosts = balance.actionCosts();
        GameBalanceProperties.Travel travel = actionCosts.travel();
        GameBalanceProperties.WorkOnProduct workOnProduct = actionCosts.workOnProduct();
        GameBalanceProperties.Marketing marketing = actionCosts.marketing();
        GameBalanceProperties.PitchVcs pitchVcs = actionCosts.pitchVcs();
        GameBalanceProperties.BuySupplies buySupplies = actionCosts.buySupplies();

        boolean hasRouteOptions = !routeService.getAvailableNextLocations(session.getCurrentLocation()).isEmpty();
        if (hasRouteOptions && session.getCoffee() >= travel.coffeeCost()) {
            actions.add(baseAction(
                    ActionType.TRAVEL,
                    travel.cashCost(),
                    travel.coffeeCost(),
                    0,
                    true,
                    false,
                    null,
                    null
            ));
        }

        actions.add(baseAction(ActionType.REST, 0, 0, 0, false, false, null, null));

        if (session.getCoffee() >= workOnProduct.coffeeCost()
                && session.getMorale() >= workOnProduct.moraleCost()) {
            actions.add(baseAction(
                    ActionType.WORK_ON_PRODUCT,
                    0,
                    workOnProduct.coffeeCost(),
                    workOnProduct.moraleCost(),
                    false,
                    false,
                    null,
                    null
            ));
        }

        actions.add(baseAction(
                ActionType.MARKETING,
                marketing.cashCost(),
                0,
                0,
                false,
                false,
                null,
                null
        ));

        if (session.getCoffee() >= pitchVcs.coffeeCost()
                && session.getMorale() >= pitchVcs.moraleCost()
                && session.getMorale() >= pitchVcs.moraleGate()) {
            actions.add(baseAction(
                    ActionType.PITCH_VCS,
                    0,
                    pitchVcs.coffeeCost(),
                    pitchVcs.moraleCost(),
                    false,
                    false,
                    null,
                    null
            ));
        }

        actions.add(baseAction(
                ActionType.BUY_SUPPLIES,
                buySupplies.cashCost(),
                0,
                0,
                false,
                false,
                null,
                null
        ));

        if (session.getCash() >= balance.crypto().minInvest()) {
            actions.add(baseAction(
                    ActionType.INVEST_CRYPTO,
                    0,
                    0,
                    0,
                    false,
                    true,
                    balance.crypto().minInvest(),
                    session.getCash()
            ));
        }

        return List.copyOf(actions);
    }

    public void validateActionLegal(GameSessionEntity session, ActionType type, boolean hasLoseActionPending) {
        if (type == ActionType.SKIP && !hasLoseActionPending) {
            throw new GameConflictException("SKIP is only legal when a forced-rest event is pending");
        }
        if (type != ActionType.SKIP && hasLoseActionPending) {
            throw new GameConflictException("Burnout Wave forces SKIP this turn");
        }

        boolean available = computeAvailableActions(session, hasLoseActionPending).stream()
                .anyMatch(action -> action.type() == type);
        if (!available) {
            throw new DomainValidationException("Action is not currently available: " + type);
        }
    }

    private static AvailableActionDto baseAction(
            ActionType type,
            int cashCost,
            int coffeeCost,
            int moraleCost,
            boolean requiresDestination,
            boolean requiresAmount,
            Integer minAmount,
            Integer maxAmount
    ) {
        return new AvailableActionDto(
                type,
                cashCost,
                coffeeCost,
                moraleCost,
                ZERO_WEATHER_SURCHARGE,
                requiresDestination,
                requiresAmount,
                minAmount,
                maxAmount,
                null
        );
    }
}
