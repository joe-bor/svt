package com.joe_bor.svt_api.services.game;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WinLossEvaluator {

    private final GameBalanceProperties balance;

    public GameEndReason evaluate(GameSessionEntity session) {
        // Win/loss priority is part of the game rules: reaching SF beats every failure condition.
        if (session.getCurrentLocation() != null
                && session.getCurrentLocation().getId() == balance.thresholds().terminalLocationId()) {
            return GameEndReason.REACHED_SF;
        }
        if (session.getCash() <= balance.thresholds().cashBankrupt()) {
            return GameEndReason.CASH_BANKRUPT;
        }
        if (session.getCustomers() <= 0) {
            return GameEndReason.CUSTOMERS_ZERO;
        }
        if (session.getMorale() <= 0) {
            return GameEndReason.MORALE_ZERO;
        }
        return null;
    }

    @Transactional
    public GameEndReason evaluateAndApply(GameSessionEntity session) {
        GameEndReason reason = evaluate(session);
        if (reason == null) {
            return null;
        }

        session.setStatus(reason == GameEndReason.REACHED_SF ? GameSessionStatus.WON : GameSessionStatus.LOST);
        session.setGameEndReason(reason);
        return reason;
    }
}
