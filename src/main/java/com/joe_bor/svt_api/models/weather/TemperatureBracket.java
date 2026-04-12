package com.joe_bor.svt_api.models.weather;

public enum TemperatureBracket {
    COLD,
    NORMAL,
    HOT;

    public static TemperatureBracket fromFahrenheit(double apparentTemperatureMaxF) {
        if (apparentTemperatureMaxF < 50.0) {
            return COLD;
        }
        if (apparentTemperatureMaxF <= 85.0) {
            return NORMAL;
        }
        return HOT;
    }
}
