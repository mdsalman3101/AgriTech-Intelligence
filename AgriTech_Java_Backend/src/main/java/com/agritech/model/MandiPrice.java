package com.agritech.model;

import java.time.LocalDate;

public record MandiPrice(
        String cropName,
        String mandiName,
        double minPrice,
        double maxPrice,
        double modalPrice,
        double changePercent,
        LocalDate priceDate
) {}
