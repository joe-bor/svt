package com.joe_bor.svt_api.controllers.catalog.dto;

public record EventChoiceDto(
        Long id,
        String label,
        int cashEffect,
        int customersEffect,
        int moraleEffect,
        int coffeeEffect,
        int bugsEffect,
        String specialEffect
) {
}
