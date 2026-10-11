package com.agritech.app;

import com.agritech.data.DemoData;
import com.agritech.engine.*;
import com.agritech.enums.SoilType;
import com.agritech.model.*;
import com.agritech.model.crop.Crop;
import com.agritech.model.equipment.FarmEquipment;
import com.agritech.repository.FileFarmerRepository;
import com.agritech.service.*;
import com.agritech.util.InputUtil;
import java.util.*;

public class Main {
    private static final Scanner SC = new Scanner(System.in);

    public static void main(String[] args) {
        FarmerService farmerService = new FarmerService(new FileFarmerRepository("data/farmers.csv"));
        for (Farmer f : DemoData.farmers()) farmerService.seed(f);

        CropCatalogService cropService = new CropCatalogService(DemoData.crops());
        WeatherService weatherService = new DemoWeatherService();
        MandiService mandiService = new MandiService(DemoData.mandiPrices());
        ProfitService profitService = new ProfitService();
        EquipmentService equipmentService = new EquipmentService(DemoData.equipment());
        LoanService loanService = new LoanService(DemoData.loans());

        CropRecommendationEngine cropEngine = new CropRecommendationEngine(profitService, mandiService);
        SowingDecisionEngine sowingEngine = new SowingDecisionEngine();

        while (true) {
            menu();
            String c = SC.nextLine().trim();
            try {
                switch (c) {
                    case "1" -> register(farmerService);
                    case "2" -> profile(farmerService);
                    case "3" -> recommend(farmerService, cropService, weatherService, cropEngine);
                    case "4" -> sowing(cropService, weatherService, sowingEngine);
                    case "5" -> profit(cropService, mandiService, profitService);
                    case "6" -> mandi(mandiService);
                    case "7" -> equipment(equipmentService);
                    case "8" -> loan(loanService);
                    case "9" -> weather(weatherService);
                    case "0" -> { System.out.println("AgriTech closed."); return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    private static void menu() {
        System.out.println();
        System.out.println("=========================================");
        System.out.println("      AGRITECH - JAVA TERMINAL BACKEND");
        System.out.println("=========================================");
        System.out.println("1. Farmer Registration");
        System.out.println("2. View Farmer Profile");
        System.out.println("3. Find Best Crops");
        System.out.println("4. Smart Sowing Advisor");
        System.out.println("5. Profit Calculator");
        System.out.println("6. Mandi Prices");
        System.out.println("7. Equipment Rental");
        System.out.println("8. Loan EMI");
        System.out.println("9. 7-Day Weather");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    private static void register(FarmerService s) {
        System.out.print("Name: "); String name = SC.nextLine();
        System.out.print("Village: "); String village = SC.nextLine();
        System.out.print("District: "); String district = SC.nextLine();
        System.out.print("State: "); String state = SC.nextLine();
        double land = InputUtil.readDouble(SC, "Land area (acres): ");
        System.out.print("Soil [LOAMY/CLAY/SANDY/SANDY_LOAM/CLAY_LOAM]: ");
        SoilType soil = SoilType.valueOf(SC.nextLine().trim().toUpperCase());
        System.out.print("Phone: "); String phone = SC.nextLine();

        Farmer f = s.register(name, village, district, state, land, soil, phone);
        System.out.println("Registered. Farmer ID = " + f.getFarmerId());
    }

    private static void profile(FarmerService s) {
        System.out.print("Farmer ID: ");
        System.out.println(s.getById(SC.nextLine().trim()));
    }

    private static void recommend(FarmerService fs, CropCatalogService cs,
                                  WeatherService ws, CropRecommendationEngine engine) {
        System.out.print("Farmer ID: ");
        Farmer f = fs.getById(SC.nextLine().trim());

        List<CropRecommendation> list = engine.recommend(
                f,
                cs.getAll(),
                ws.getSevenDayForecast(f.getDistrict(), f.getState())
        );

        System.out.println("\nTOP 3 CROP RECOMMENDATIONS");
        int rank = 1;
        for (CropRecommendation r : list) {
            System.out.println("--------------------------------");
            System.out.println("#" + rank++ + " " + r.crop().getName());
            System.out.printf("Score: %.1f/100%n", r.finalScore());
            System.out.printf("Estimated Profit: Rs %.2f%n", r.estimatedProfit());
            System.out.println("Risk: " + r.riskLevel());
            System.out.println("Why: " + r.explanation());
        }
    }

    private static void sowing(CropCatalogService cs, WeatherService ws, SowingDecisionEngine engine) {
        System.out.print("Crop name [Wheat/Rice/Maize/Mustard]: ");
        Crop crop = cs.findByName(SC.nextLine());
        System.out.print("District: "); String district = SC.nextLine();
        System.out.print("State: "); String state = SC.nextLine();

        SowingAdvisory a = engine.advise(crop, ws.getSevenDayForecast(district, state));

        System.out.println("Status: " + a.status());
        System.out.println("Risk: " + a.riskLevel());
        if (a.recommendedStart() != null)
            System.out.println("Window: " + a.recommendedStart() + " to " + a.recommendedEnd());
        System.out.println("Reason: " + a.explanation());
    }

    private static void profit(CropCatalogService cs, MandiService ms, ProfitService ps) {
        System.out.print("Crop name: ");
        Crop crop = cs.findByName(SC.nextLine());
        double area = InputUtil.readDouble(SC, "Land area: ");
        MandiPrice p = ms.getByCrop(crop.getName());

        System.out.printf("Mandi Price: Rs %.2f/q%n", p.modalPrice());
        System.out.printf("Cost: Rs %.2f%n", ps.cost(crop, area));
        System.out.printf("Revenue: Rs %.2f%n", ps.revenue(crop, area, p.modalPrice()));
        System.out.printf("Profit: Rs %.2f%n", ps.profit(crop, area, p.modalPrice()));
        System.out.printf("Profit/Acre: Rs %.2f%n", ps.profitPerAcre(crop, area, p.modalPrice()));
    }

    private static void mandi(MandiService s) {
        for (MandiPrice p : s.getAll()) {
            System.out.printf("%s | %s | Rs %.2f/q | Change %.2f%%%n",
                    p.cropName(), p.mandiName(), p.modalPrice(), p.changePercent());
        }
    }

    private static void equipment(EquipmentService s) {
        for (FarmEquipment e : s.getAll()) {
            System.out.printf("%s | %s | Rs %.2f/day | Units %d%n",
                    e.getId(), e.getName(), e.getDailyRate(), e.getAvailableUnits());
        }
        System.out.print("Equipment ID: ");
        String id = SC.nextLine();
        int days = InputUtil.readInt(SC, "Days: ");
        System.out.printf("Booking successful. Total Rs %.2f%n", s.book(id, days));
    }

    private static void loan(LoanService s) {
        System.out.println("DEMO EXISTING LOANS");
        for (Loan l : s.getDemoLoans()) {
            System.out.printf("%s | Principal Rs %.2f | EMI Rs %.2f | Remaining Rs %.2f%n",
                    l.getType(), l.getPrincipal(), l.calculateEmi(), l.estimatedRemainingAmount());
        }

        System.out.print("New loan type: "); String type = SC.nextLine();
        double p = InputUtil.readDouble(SC, "Principal: ");
        double r = InputUtil.readDouble(SC, "Annual interest %: ");
        int m = InputUtil.readInt(SC, "Tenure months: ");
        System.out.printf("Monthly EMI = Rs %.2f%n", s.create(type, p, r, m).calculateEmi());
    }

    private static void weather(WeatherService s) {
        System.out.print("District: "); String district = SC.nextLine();
        System.out.print("State: "); String state = SC.nextLine();

        for (DailyForecast d : s.getSevenDayForecast(district, state)) {
            System.out.printf("%s | %.1f-%.1f C | Rain %.1f mm | Chance %.0f%% | Humidity %.0f%% | Wind %.1f km/h%n",
                    d.date(), d.minTemperature(), d.maxTemperature(),
                    d.rainMm(), d.rainProbability(), d.humidity(), d.windSpeed());
        }
    }
}
