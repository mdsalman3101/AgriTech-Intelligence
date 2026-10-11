package com.agritech.model.crop;

import com.agritech.enums.SoilType;
import com.agritech.enums.WaterRequirement;
import java.util.Set;

public class Rice extends Crop {
    public Rice() {
        super("Rice", 135, 20, 35, 22, 2700,
                WaterRequirement.HIGH, Set.of(SoilType.CLAY, SoilType.CLAY_LOAM));
    }

    @Override
    public double calculateCultivationCost(double acres) {
        return 26000 * acres;
    }
}
