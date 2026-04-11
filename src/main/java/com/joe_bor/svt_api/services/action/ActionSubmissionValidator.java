package com.joe_bor.svt_api.services.action;

import com.joe_bor.svt_api.common.DomainValidationException;
import com.joe_bor.svt_api.common.GameConflictException;
import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.controllers.action.dto.EventChoiceSelectionDto;
import com.joe_bor.svt_api.controllers.action.dto.SubmitActionRequest;
import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.SpecialEffectType;
import com.joe_bor.svt_api.models.gameplay.ActionType;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionSubmissionValidator {

    private final GameBalanceProperties balance;

    public ValidatedActionSubmission validate(
            GameSessionEntity session,
            SubmitActionRequest request,
            List<EventEntity> pendingEvents
    ) {
        if (session.getStatus() != GameSessionStatus.IN_PROGRESS) {
            throw new GameConflictException("Cannot submit action on a finished game");
        }
        if (pendingEvents.isEmpty()) {
            throw new GameConflictException("No pending turn; call POST /turns/next first");
        }

        boolean hasLoseActionPending = pendingEvents.stream()
                .anyMatch(event -> event.getSpecialEffect() == SpecialEffectType.LOSE_ACTION);
        validateActionPayload(session, request.action(), hasLoseActionPending);

        // Choice submissions must line up exactly with the unresolved "choose one" events for this turn.
        List<Long> expectedChoiceEventIds = pendingEvents.stream()
                .filter(EventEntity::isHasChoice)
                .map(EventEntity::getId)
                .sorted()
                .toList();
        List<Long> providedChoiceEventIds = request.eventChoices().stream()
                .map(EventChoiceSelectionDto::eventId)
                .sorted()
                .toList();
        if (!expectedChoiceEventIds.equals(providedChoiceEventIds)) {
            throw new GameConflictException("Event choice mismatch: expected=" + expectedChoiceEventIds
                    + ", got=" + providedChoiceEventIds);
        }

        Map<Long, EventChoiceSelectionDto> selectionsByEventId = request.eventChoices().stream()
                .collect(Collectors.toMap(
                        EventChoiceSelectionDto::eventId,
                        Function.identity(),
                        (left, right) -> {
                            throw new GameConflictException("Event choice mismatch: expected="
                                    + expectedChoiceEventIds + ", got=" + providedChoiceEventIds);
                        }
                ));

        Map<Long, EventChoiceEntity> resolvedChoices = pendingEvents.stream()
                .filter(EventEntity::isHasChoice)
                .collect(Collectors.toMap(
                        EventEntity::getId,
                        event -> resolveChoice(event, selectionsByEventId.get(event.getId()))
                ));

        return new ValidatedActionSubmission(hasLoseActionPending, resolvedChoices);
    }

    // Resolves a submitted choice against the seeded options for that specific pending event.
    private EventChoiceEntity resolveChoice(EventEntity event, EventChoiceSelectionDto selection) {
        return event.getChoices().stream()
                .filter(choice -> choice.getId().equals(selection.choiceId()))
                .min(Comparator.comparing(EventChoiceEntity::getId))
                .orElseThrow(() -> new GameConflictException(
                        "Event choice mismatch: expected=" + List.of(event.getId()) + ", got=" + List.of(selection.eventId())));
    }

    // Enforces per-action request fields before the more expensive post-event legality check runs.
    private void validateActionPayload(
            GameSessionEntity session,
            SubmitActionRequest.ActionPayload action,
            boolean hasLoseActionPending
    ) {
        // Burnout is a turn-level override: once it is pending, every normal action is blocked.
        if (action.type() == ActionType.SKIP && !hasLoseActionPending) {
            throw new GameConflictException("SKIP is only legal when a forced-rest event is pending");
        }
        if (action.type() != ActionType.SKIP && hasLoseActionPending) {
            throw new GameConflictException("Burnout Wave forces SKIP this turn");
        }

        switch (action.type()) {
            case TRAVEL -> {
                if (action.destinationLocationId() == null) {
                    throw new DomainValidationException("destinationLocationId is required for TRAVEL");
                }
                if (action.amount() != null) {
                    throw new DomainValidationException("amount is only allowed for INVEST_CRYPTO");
                }
            }
            case INVEST_CRYPTO -> {
                if (action.amount() == null) {
                    throw new DomainValidationException("amount is required for INVEST_CRYPTO");
                }
                if (action.destinationLocationId() != null) {
                    throw new DomainValidationException("destinationLocationId is only allowed for TRAVEL");
                }
                if (action.amount() < balance.crypto().minInvest()) {
                    throw new DomainValidationException("Invest amount must be at least " + balance.crypto().minInvest());
                }
                if (action.amount() > session.getCash()) {
                    throw new DomainValidationException("Invest amount exceeds current cash");
                }
            }
            default -> {
                if (action.destinationLocationId() != null) {
                    throw new DomainValidationException("destinationLocationId is only allowed for TRAVEL");
                }
                if (action.amount() != null) {
                    throw new DomainValidationException("amount is only allowed for INVEST_CRYPTO");
                }
            }
        }
    }

    public record ValidatedActionSubmission(
            boolean hasLoseActionPending,
            Map<Long, EventChoiceEntity> selectedChoicesByEventId
    ) {
    }
}
