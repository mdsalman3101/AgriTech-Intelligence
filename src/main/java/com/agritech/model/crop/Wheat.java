package com.agritech.model.crop;

import com.agritech.enums.SoilType;
import com.agritech.enums.WaterRequirement;
import java.util.Set;

public class Wheat extends Crop {
    public Wheat() {
        super("Wheat", 120, 10, 27, 18, 2450,
                WaterRequirement.MEDIUM, Set.of(SoilType.LOAMY, SoilType.CLAY_LOAM));
    }

    @Override
    public double calculateCultivationCost(double acres) {
        return 17000 * acres;
    }
}
