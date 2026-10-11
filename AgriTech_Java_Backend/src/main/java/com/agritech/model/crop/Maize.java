package com.agritech.model.crop;

import com.agritech.enums.SoilType;
import com.agritech.enums.WaterRequirement;
import java.util.Set;

public class Maize extends Crop {
    public Maize() {
        super("Maize", 100, 18, 32, 20, 2250,
                WaterRequirement.MEDIUM, Set.of(SoilType.LOAMY, SoilType.SANDY_LOAM));
    }

    @Override
    public double calculateCultivationCost(double acres) {
        return 18000 * acres;
    }
}
