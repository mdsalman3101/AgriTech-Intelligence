package com.agritech.model;

import com.agritech.enums.RiskLevel;
import com.agritech.model.crop.Crop;

public record CropRecommendation(
        Crop crop,
        double finalScore,
        double estimatedProfit,
        RiskLevel riskLevel,
        String explanation
) {}
