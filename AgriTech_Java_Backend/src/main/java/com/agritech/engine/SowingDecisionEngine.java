package com.agritech.engine;

import com.agritech.enums.RiskLevel;
import com.agritech.model.*;
import com.agritech.model.crop.Crop;
import java.util.List;

public class SowingDecisionEngine {
    public SowingAdvisory advise(Crop crop, List<DailyForecast> forecast) {
        if (forecast == null || forecast.isEmpty())
            throw new IllegalArgumentException("Forecast required.");

        for (int i = 0; i < forecast.size(); i++) {
            DailyForecast day = forecast.get(i);

            boolean tempOk = day.maxTemperature() >= crop.getMinTemperature()
                    && day.maxTemperature() <= crop.getMaxTemperature();

            boolean heavyRain = day.rainMm() > 25 || day.rainProbability() >= 85;
            boolean nextDayRisk = false;

            if (i + 1 < forecast.size()) {
                DailyForecast next = forecast.get(i + 1);
                nextDayRisk = next.rainMm() > 30 || next.rainProbability() >= 90;
            }

            if (tempOk && !heavyRain && !nextDayRisk) {
                return new SowingAdvisory(
                        crop.getName(),
                        "RECOMMENDED",
                        day.date(),
                        day.date().plusDays(1),
                        RiskLevel.LOW,
                        "Temperature is suitable and severe rain risk is not detected."
                );
            }
        }

        return new SowingAdvisory(
                crop.getName(),
                "WAIT",
                null,
                null,
                RiskLevel.HIGH,
                "No low-risk sowing window found in current 7-day demo forecast."
        );
    }
}
