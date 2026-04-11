package com.joe_bor.svt_api.services.economy;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassiveDrainService {

    private final GameBalanceProperties balance;

    @Transactional
    public int applyCoffeeDecay(GameSessionEntity session) {
        int updatedCoffee = Math.max(0, session.getCoffee() - balance.economy().coffeeDecayPerTurn());
        int delta = updatedCoffee - session.getCoffee();
        session.setCoffee(updatedCoffee);
        return delta;
    }
}
