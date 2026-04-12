package com.joe_bor.svt_api.models.weather;

public enum WeatherBucket {
    CLEAR,
    FOGGY,
    RAINY,
    STORMY;

    public static WeatherBucket fromWmoCode(int weatherCode) {
        if (weatherCode >= 0 && weatherCode <= 1) {
            return CLEAR;
        }
        if ((weatherCode >= 2 && weatherCode <= 3) || weatherCode == 45 || weatherCode == 48) {
            return FOGGY;
        }
        if ((weatherCode >= 51 && weatherCode <= 67) || (weatherCode >= 80 && weatherCode <= 82)) {
            return RAINY;
        }
        if ((weatherCode >= 71 && weatherCode <= 77) || (weatherCode >= 95 && weatherCode <= 99)) {
            return STORMY;
        }

        throw new IllegalArgumentException("Unsupported WMO weather code: " + weatherCode);
    }
}
