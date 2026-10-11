package com.agritech.model;

import com.agritech.enums.RiskLevel;
import java.time.LocalDate;

public record SowingAdvisory(
        String cropName,
        String status,
        LocalDate recommendedStart,
        LocalDate recommendedEnd,
        RiskLevel riskLevel,
        String explanation
) {}
