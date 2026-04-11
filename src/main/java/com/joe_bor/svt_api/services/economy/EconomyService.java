package com.joe_bor.svt_api.services.economy;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EconomyService {

    private final GameBalanceProperties balance;

    @Transactional
    public int applyPassiveEconomy(GameSessionEntity session) {
        // Money passives live here: customers generate turn revenue, then the company pays its fixed operating burn.
        int delta = (session.getCustomers() * balance.economy().revenuePerCustomer())
                - balance.economy().operatingCost();
        session.setCash(session.getCash() + delta);
        return delta;
    }

    @Transactional
    public int applyCryptoSettlement(GameSessionEntity session) {
        Integer pendingSettlement = session.getPendingCryptoSettlement();
        if (pendingSettlement == null) {
            return 0;
        }

        // Crypto investments settle at the start of a later turn, before the player chooses a new action.
        session.setCash(session.getCash() + pendingSettlement);
        session.setPendingCryptoSettlement(null);
        return pendingSettlement;
    }
}
