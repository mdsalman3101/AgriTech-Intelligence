package com.agritech.model;

import java.time.LocalDate;

public record DailyForecast(
        LocalDate date,
        double minTemperature,
        double maxTemperature,
        double rainMm,
        double rainProbability,
        double humidity,
        double windSpeed
) {}
