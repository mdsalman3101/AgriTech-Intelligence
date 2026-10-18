package com.agritech.narrator;

import com.agritech.model.MandiPrice;
import com.agritech.model.ProfitEstimate;
import com.agritech.model.SmartRecommendation;

public class LocalAdvisoryNarrator implements AdvisoryNarrator {
    @Override
    public String narrate(SmartRecommendation r) {
        StringBuilder out = new StringBuilder();

        out.append("Namaste! Aapke farm ke current data ke hisaab se ")
                .append(r.crop().getName())
                .append(" ko analyze kiya gaya hai.\n\n");

        out.append("Kyun?\n");
        out.append("- Soil suitability: ").append(Math.round(r.soilScore())).append("/100\n");
        out.append("- Weather suitability: ").append(Math.round(r.weatherScore())).append("/100\n");
        out.append("- Water suitability: ").append(Math.round(r.waterScore())).append("/100\n");
        out.append("- Sowing safety: ").append(r.sowing().suitabilityScore()).append("/100\n");

        MandiPrice mandi = r.mandi();
        if (mandi != null) {
            out.append("- Latest available mandi modal price: Rs ")
                    .append(String.format("%.2f", mandi.modalPrice()))
                    .append("/quintal at ").append(mandi.mandiName())
                    .append(" (data date: ").append(mandi.priceDate()).append(")\n");
        } else {
            out.append("- Market data abhi available nahi hai; score agronomic evidence par based hai.\n");
        }

        out.append("\nSowing Advice:\n")
                .append(r.sowing().farmerAction())
                .append("\nReason: ")
                .append(r.sowing().explanation())
                .append("\nRisk: ")
                .append(r.risk())
                .append("\n");

        ProfitEstimate economics = r.economics();
        if (economics != null) {
            out.append("\nMarket/Profit Note:\n")
                    .append("Reference-based estimated profit Rs ")
                    .append(economics.profit())
                    .append(" hai. Ye guaranteed income nahi hai.\n");
        } else {
            out.append("\nMarket/Profit Note:\n")
                    .append("Genuine current mandi price available nahi hua, isliye profit estimate include nahi kiya gaya.\n");
        }

        out.append("\nAb kya karein?\n")
                .append(r.sowing().farmerAction());

        return out.toString();
    }
}
