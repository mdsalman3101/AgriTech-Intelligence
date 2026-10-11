package com.agritech.model.crop;

import com.agritech.enums.SoilType;
import com.agritech.enums.WaterRequirement;
import java.util.Set;

public class Mustard extends Crop {
    public Mustard() {
        super("Mustard", 110, 10, 30, 8, 5300,
                WaterRequirement.LOW, Set.of(SoilType.LOAMY, SoilType.SANDY_LOAM));
    }

    @Override
    public double calculateCultivationCost(double acres) {
        return 15000 * acres;
    }
}
