package com.joe_bor.svt_api.integration.crypto;

import java.time.LocalDate;
import java.time.ZoneId;

public interface CryptoClient {

    double fetchDelta(LocalDate startDate, LocalDate endDate, ZoneId timezone);
}
