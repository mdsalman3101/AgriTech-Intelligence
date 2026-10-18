package com.agritech.engine;

import com.agritech.enums.RiskLevel;
import com.agritech.model.DailyForecast;
import com.agritech.model.SowingAdvisory;
import com.agritech.model.crop.Crop;

import java.util.List;

public class SowingDecisionEngine {
    public SowingAdvisory advise(Crop crop, List<DailyForecast> forecast) {
        if (crop == null) throw new IllegalArgumentException("Crop required hai.");
        if (forecast == null || forecast.isEmpty()) {
            throw new IllegalArgumentException("Sowing advice ke liye real weather forecast required hai.");
        }

        int bestIndex = -1;
        int bestScore = -1;

        for (int i = 0; i < forecast.size(); i++) {
            DailyForecast day = forecast.get(i);
            int score = scoreDay(crop, forecast, i, day);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        bestScore = Math.max(0, Math.min(100, bestScore));
        RiskLevel risk = bestScore >= 75 ? RiskLevel.LOW
                : bestScore >= 50 ? RiskLevel.MEDIUM
                : RiskLevel.HIGH;

        String status;
        if (bestIndex == 0 && bestScore >= 70) status = "RECOMMENDED";
        else if (bestScore >= 50) status = "WAIT";
        else status = "AVOID";

        var start = bestIndex >= 0 && bestScore >= 50 ? forecast.get(bestIndex).date() : null;
        var end = start == null ? null : start.plusDays(1);

        String why;
        String action;

        if (status.equals("RECOMMENDED")) {
            why = "Temperature crop ke range me hai aur severe rain/wind risk detect nahi hua.";
            action = "Field preparation complete rakhiye aur recommended window me sowing kijiye. "
                    + "Field work se pehle forecast ek baar phir check kar lijiye.";
        } else if (status.equals("WAIT")) {
            why = "Aaj ke comparison me next 7 days me ek safer weather window mil rahi hai.";
            action = "Abhi wait kijiye. Recommended window ke paas forecast dobara check karke sowing kijiye.";
        } else {
            why = "Current 7-day forecast me acceptably safe sowing window nahi mil rahi.";
            action = "Is week sowing avoid kijiye aur weather change hone par fresh advice lijiye.";
        }

        return new SowingAdvisory(
                crop.getName(), status, start, end, risk, bestScore, why, action
        );
    }

    private int scoreDay(Crop crop, List<DailyForecast> forecast, int index, DailyForecast day) {
        double mean = day.averageTemperature();
        int score = 100;

        if (mean < crop.getMinTemperature() || mean > crop.getMaxTemperature()) score -= 45;
        if (day.rainMm() > 25 || day.rainProbability() >= 85) score -= 40;

        if (index + 1 < forecast.size()) {
            DailyForecast next = forecast.get(index + 1);
            if (next.rainMm() > 30 || next.rainProbability() >= 90) score -= 35;
        }

        if (day.windSpeed() > 35) score -= 15;
        if (day.humidity() > 90) score -= 10;

        return Math.max(0, Math.min(100, score));
    }
}
