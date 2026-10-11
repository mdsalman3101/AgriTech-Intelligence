package com.agritech.service;

import com.agritech.model.MandiPrice;
import java.util.List;

public class MandiService {
    private final List<MandiPrice> prices;

    public MandiService(List<MandiPrice> prices) {
        this.prices = prices;
    }

    public List<MandiPrice> getAll() { return List.copyOf(prices); }

    public MandiPrice getByCrop(String crop) {
        return prices.stream()
                .filter(p -> p.cropName().equalsIgnoreCase(crop))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mandi price not found."));
    }
}
