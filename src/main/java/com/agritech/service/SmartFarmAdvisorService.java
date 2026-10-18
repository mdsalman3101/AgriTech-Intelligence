package com.agritech.service;

import com.agritech.engine.SowingDecisionEngine;
import com.agritech.enums.RiskLevel;
import com.agritech.model.DailyForecast;
import com.agritech.model.Farmer;
import com.agritech.model.MandiPrice;
import com.agritech.model.ProfitEstimate;
import com.agritech.model.SmartRecommendation;
import com.agritech.model.SowingAdvisory;
import com.agritech.model.crop.Crop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SmartFarmAdvisorService {
    private final MandiService mandi;
    private final ProfitService profits;
    private final SowingDecisionEngine sowing = new SowingDecisionEngine();

    public SmartFarmAdvisorService(MandiService mandi, ProfitService profits) {
        this.mandi = mandi;
        this.profits = profits;
    }

    public List<SmartRecommendation> recommend(Farmer farmer, List<Crop> crops, List<DailyForecast> forecast) {
        if (farmer == null) throw new IllegalArgumentException("Farmer profile required hai.");
        if (forecast == null || forecast.isEmpty()) {
            throw new IllegalArgumentException("Crop advice ke liye real weather forecast required hai.");
        }

        List<SmartRecommendation> out = new ArrayList<>();

        for (Crop crop : crops) {
            double soilScore = crop.supportsSoil(farmer.getSoilType()) ? 100 : 20;
            double weatherScore = weatherScore(crop, forecast);
            double waterScore = waterScore(crop, farmer, forecast);

            // Critical safety gate. A high market price must never override unsafe agronomy.
            if (soilScore < 30 || weatherScore < 40) continue;

            SowingAdvisory sowingAdvice = sowing.advise(crop, forecast);
            double safetyScore = sowingAdvice.suitabilityScore();

            MandiPrice marketPrice = null;
            ProfitEstimate economics = null;
            Double profitabilityScore = null;
            Double marketScore = null;

            try {
                marketPrice = mandi.bestPrice(crop.getName(), farmer.getState(), farmer.getDistrict());
                economics = profits.calculate(
                        farmer.getLandArea(),
                        crop.getExpectedYieldPerAcre(),
                        BigDecimal.valueOf(marketPrice.modalPrice()),
                        BigDecimal.valueOf(crop.calculateCultivationCost(1))
                );
                profitabilityScore = profitabilityScore(economics);
                marketScore = marketScore(marketPrice);
            } catch (RuntimeException ignored) {
                // No fake price fallback. Recommendation remains agronomic-only.
            }

            double weighted = soilScore * 0.25
                    + weatherScore * 0.20
                    + waterScore * 0.10
                    + safetyScore * 0.10;
            double availableWeight = 0.65;

            if (profitabilityScore != null && marketScore != null) {
                weighted += profitabilityScore * 0.25 + marketScore * 0.10;
                availableWeight = 1.0;
            }

            double finalScore = clamp(weighted / availableWeight);
            RiskLevel risk = finalScore >= 80 ? RiskLevel.LOW
                    : finalScore >= 60 ? RiskLevel.MEDIUM
                    : RiskLevel.HIGH;

            String evidenceNote = marketPrice == null
                    ? "Market/profit evidence unavailable tha, isliye score agronomic evidence par normalized hai."
                    : "Latest available government mandi evidence market/profit scoring me include hua.";

            String explanation = "Soil " + Math.round(soilScore) + "/100, weather "
                    + Math.round(weatherScore) + "/100, water " + Math.round(waterScore)
                    + "/100, sowing safety " + Math.round(safetyScore) + "/100. " + evidenceNote;

            String label = label(finalScore);
            if (marketPrice == null) label += " (Agronomic)";

            out.add(new SmartRecommendation(
                    crop, finalScore, label, risk,
                    soilScore, weatherScore, waterScore,
                    sowingAdvice, marketPrice, economics, explanation
            ));
        }

        return out.stream()
                .sorted(Comparator.comparingDouble(SmartRecommendation::score).reversed())
                .limit(3)
                .toList();
    }

    private double weatherScore(Crop crop, List<DailyForecast> forecast) {
        double score = 0;
        for (DailyForecast day : forecast) {
            double mean = day.averageTemperature();
            double dayScore = (mean >= crop.getMinTemperature() && mean <= crop.getMaxTemperature()) ? 100 : 45;
            if (day.rainMm() >= 30 || day.rainProbability() >= 90) dayScore -= 25;
            if (day.windSpeed() >= 35) dayScore -= 15;
            score += Math.max(0, dayScore);
        }
        return clamp(score / forecast.size());
    }

    private double waterScore(Crop crop, Farmer farmer, List<DailyForecast> forecast) {
        double rain = forecast.stream().mapToDouble(DailyForecast::rainMm).sum();
        return switch (crop.getWaterRequirement()) {
            case LOW -> rain <= 50 ? 90 : 60;
            case MEDIUM -> farmer.isIrrigationAvailable() ? 90 : (rain >= 15 ? 75 : 60);
            case HIGH -> farmer.isIrrigationAvailable() ? 85 : (rain >= 30 ? 65 : 25);
        };
    }

    private double profitabilityScore(ProfitEstimate estimate) {
        if (estimate == null || estimate.revenue().signum() <= 0) return 20;
        if (estimate.profit().signum() <= 0) return 25;

        double margin = estimate.profit().doubleValue() / estimate.revenue().doubleValue() * 100.0;
        if (margin >= 30) return 95;
        if (margin >= 20) return 85;
        if (margin >= 10) return 70;
        return 55;
    }

    private double marketScore(MandiPrice price) {
        double score = price.exactDistrict() ? 90 : 70;
        if (price.cached()) score -= 5;
        if (price.priceDate() != null) {
            long daysOld = Math.abs(ChronoUnit.DAYS.between(price.priceDate(), LocalDate.now()));
            if (daysOld > 7) score -= 10;
            if (daysOld > 30) score -= 10;
        }
        return clamp(score);
    }

    private double clamp(double score) {
        return Math.max(0, Math.min(100, score));
    }

    private String label(double score) {
        if (score >= 85) return "Highly Suitable";
        if (score >= 70) return "Suitable";
        if (score >= 55) return "Moderate";
        if (score >= 40) return "High Risk";
        return "Not Recommended";
    }
}
