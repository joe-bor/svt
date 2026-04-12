package com.joe_bor.svt_api.integration.weather;

import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
import java.time.LocalDate;
import java.time.ZoneId;

public interface WeatherClient {

    WeatherSnapshot fetch(double latitude, double longitude, LocalDate date, ZoneId timezone);
}
