package com.agritech.engine;

import com.agritech.enums.*;
import com.agritech.model.*;
import com.agritech.model.crop.Crop;
import com.agritech.service.*;
import java.util.*;

public class CropRecommendationEngine {
    private final ProfitService profitService;
    private final MandiService mandiService;

    public CropRecommendationEngine(ProfitService profitService, MandiService mandiService) {
        this.profitService = profitService;
        this.mandiService = mandiService;
    }

    public List<CropRecommendation> recommend(Farmer farmer, List<Crop> crops, List<DailyForecast> forecast) {
        double avgTemp = forecast.stream().mapToDouble(DailyForecast::maxTemperature).average().orElse(25);
        double totalRain = forecast.stream().mapToDouble(DailyForecast::rainMm).sum();
        List<CropRecommendation> result = new ArrayList<>();

        for (Crop crop : crops) {
            double soil = crop.supportsSoil(farmer.getSoilType()) ? 95 : 35;
            double weather = avgTemp >= crop.getMinTemperature() && avgTemp <= crop.getMaxTemperature() ? 90 : 50;
            double water = waterScore(crop.getWaterRequirement(), totalRain);

            MandiPrice p = mandiService.getByCrop(crop.getName());
            double estimatedProfit = profitService.profit(crop, farmer.getLandArea(), p.modalPrice());
            double profitScore = estimatedProfit <= 0 ? 35 : Math.min(100, 50 + estimatedProfit / 6000.0);
            double market = p.changePercent() >= 0 ? 85 : 60;
            double safety = (soil < 40 || weather < 55) ? 40 : 88;

            double score = soil * .25 + weather * .20 + water * .10 + profitScore * .25 + market * .10 + safety * .10;
            RiskLevel risk = score >= 80 ? RiskLevel.LOW : score >= 65 ? RiskLevel.MEDIUM : RiskLevel.HIGH;

            String why = "Soil=" + (int)soil + ", Weather=" + (int)weather +
                    ", Water=" + (int)water + ", Market=" + (int)market +
                    ", Profit Rs " + String.format("%.0f", estimatedProfit);

            result.add(new CropRecommendation(crop, score, estimatedProfit, risk, why));
        }

        return result.stream()
                .sorted(Comparator.comparingDouble(CropRecommendation::finalScore).reversed())
                .limit(3)
                .toList();
    }

    private double waterScore(WaterRequirement req, double rain) {
        return switch (req) {
            case HIGH -> rain >= 40 ? 90 : 45;
            case MEDIUM -> (rain >= 15 && rain <= 70) ? 88 : 60;
            case LOW -> rain <= 50 ? 90 : 60;
        };
    }
}
