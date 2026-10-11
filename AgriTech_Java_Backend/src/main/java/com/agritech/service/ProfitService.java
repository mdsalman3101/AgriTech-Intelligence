package com.agritech.service;

import com.agritech.model.crop.Crop;

public class ProfitService {
    public double cost(Crop crop, double area) {
        if (area <= 0) throw new IllegalArgumentException("Area must be > 0.");
        return crop.calculateCultivationCost(area);
    }

    public double revenue(Crop crop, double area, double marketPrice) {
        return crop.calculateRevenue(area, marketPrice);
    }

    public double profit(Crop crop, double area, double marketPrice) {
        return revenue(crop, area, marketPrice) - cost(crop, area);
    }

    public double profitPerAcre(Crop crop, double area, double marketPrice) {
        return profit(crop, area, marketPrice) / area;
    }
}
