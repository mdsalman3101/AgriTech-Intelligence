package com.agritech.service;

import com.agritech.model.crop.Crop;
import java.util.List;

public class CropCatalogService {
    private final List<Crop> crops;

    public CropCatalogService(List<Crop> crops) {
        this.crops = crops;
    }

    public List<Crop> getAll() { return List.copyOf(crops); }

    public Crop findByName(String name) {
        return crops.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Crop not found."));
    }
}
