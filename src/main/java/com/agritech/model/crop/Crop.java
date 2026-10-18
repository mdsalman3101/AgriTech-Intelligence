package com.agritech.model.crop;

import com.agritech.enums.SoilType;
import com.agritech.enums.WaterRequirement;
import java.util.Set;

public abstract class Crop {
    private final String name;
    private final int growingDays;
    private final double minTemperature;
    private final double maxTemperature;
    private final double expectedYieldPerAcre;
    private final double baseMarketPrice;
    private final WaterRequirement waterRequirement;
    private final Set<SoilType> suitableSoils;

    protected Crop(String name, int growingDays, double minTemperature, double maxTemperature,
                   double expectedYieldPerAcre, double baseMarketPrice,
                   WaterRequirement waterRequirement, Set<SoilType> suitableSoils) {
        this.name = name;
        this.growingDays = growingDays;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.expectedYieldPerAcre = expectedYieldPerAcre;
        this.baseMarketPrice = baseMarketPrice;
        this.waterRequirement = waterRequirement;
        this.suitableSoils = suitableSoils;
    }

    public String getName() { return name; }
    public int getGrowingDays() { return growingDays; }
    public double getMinTemperature() { return minTemperature; }
    public double getMaxTemperature() { return maxTemperature; }
    public double getExpectedYieldPerAcre() { return expectedYieldPerAcre; }
    public double getBaseMarketPrice() { return baseMarketPrice; }
    public WaterRequirement getWaterRequirement() { return waterRequirement; }
    public Set<SoilType> getSuitableSoils() { return suitableSoils; }

    public boolean supportsSoil(SoilType soilType) {
        return suitableSoils.contains(soilType);
    }

    public double calculateRevenue(double acres, double marketPrice) {
        return expectedYieldPerAcre * acres * marketPrice;
    }

    public abstract double calculateCultivationCost(double acres);

    public double calculateProfit(double acres, double marketPrice) {
        return calculateRevenue(acres, marketPrice) - calculateCultivationCost(acres);
    }
}
