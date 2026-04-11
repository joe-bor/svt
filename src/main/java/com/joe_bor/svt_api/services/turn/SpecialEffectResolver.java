package com.joe_bor.svt_api.services.turn;

import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.SpecialEffectType;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.services.random.RandomProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecialEffectResolver {

    private static final long MUTINY_RANDOM_CHOICE_ID = 14L;
    private static final long SAND_HILL_RANDOM_CHOICE_ID = 21L;
    private static final long ACQUI_HIRE_EVENT_ID = 7L;
    private static final long ACQUI_HIRE_CHOICE_ID = 7L;
    private static final long LINKEDIN_EVENT_ID = 14L;
    private static final long LINKEDIN_GAME_OVER_CHOICE_ID = 15L;

    private final RandomProvider randomProvider;

    @Transactional
    public SpecialEffectResult resolve(GameSessionEntity session, EventEntity event, EventChoiceEntity choice) {
        SpecialEffectType specialEffect = choice.getSpecialEffect();
        if (specialEffect == null || specialEffect == SpecialEffectType.LOSE_ACTION) {
            return new SpecialEffectResult(false, null);
        }

        return switch (specialEffect) {
            case GAME_OVER -> resolveGameOver(session, event, choice);
            case RANDOM_5050 -> resolveRandomOutcome(session, choice);
            case LINKEDIN_BONUS -> {
                session.setLinkedinBonusActive(true);
                yield new SpecialEffectResult(false, "LinkedIn bonus activated");
            }
            case LOSE_ACTION -> new SpecialEffectResult(false, null);
        };
    }

    private SpecialEffectResult resolveGameOver(GameSessionEntity session, EventEntity event, EventChoiceEntity choice) {
        session.setStatus(GameSessionStatus.LOST);

        String dynamicNote = null;
        if (event.getId() == ACQUI_HIRE_EVENT_ID && choice.getId() == ACQUI_HIRE_CHOICE_ID) {
            session.setGameEndReason(GameEndReason.ACQUIRED);
            dynamicNote = "Accepted Acqui-hire Offer";
        } else if (event.getId() == LINKEDIN_EVENT_ID && choice.getId() == LINKEDIN_GAME_OVER_CHOICE_ID) {
            session.setGameEndReason(GameEndReason.TOOK_LINKEDIN_JOB);
            dynamicNote = "Accepted LinkedIn job";
        }

        return new SpecialEffectResult(true, dynamicNote);
    }

    private SpecialEffectResult resolveRandomOutcome(GameSessionEntity session, EventChoiceEntity choice) {
        boolean positive = randomProvider.nextBoolean();

        if (choice.getId() == MUTINY_RANDOM_CHOICE_ID) {
            session.setMorale(session.getMorale() + (positive ? 15 : -10));
        } else if (choice.getId() == SAND_HILL_RANDOM_CHOICE_ID) {
            if (positive) {
                session.setCash(session.getCash() + 4_000);
                session.setCustomers(session.getCustomers() + 4);
            } else {
                session.setMorale(session.getMorale() - 15);
            }
        }

        return new SpecialEffectResult(false, "50/50 resolved " + (positive ? "positive" : "negative"));
    }

    public record SpecialEffectResult(
            boolean triggeredGameOver,
            String dynamicNote
    ) {
    }
}
