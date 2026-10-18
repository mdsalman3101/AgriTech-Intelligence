package com.agritech.model;

import java.time.LocalDate;

public record MandiPrice(
        String cropName, String state, String district,
        String mandiName,
        double minPrice,
        double maxPrice,
        double modalPrice,
        double changePercent,
        LocalDate priceDate, String variety, String source, boolean exactDistrict,
        boolean cached, java.time.Instant retrievedAt
) {}
