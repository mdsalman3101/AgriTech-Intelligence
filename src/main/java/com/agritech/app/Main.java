package com.agritech.app;

import com.agritech.config.ConfigLoader;
import com.agritech.database.DatabaseManager;
import com.agritech.engine.SowingDecisionEngine;
import com.agritech.enums.SoilType;
import com.agritech.exception.ApiException;
import com.agritech.model.DailyForecast;
import com.agritech.model.Farmer;
import com.agritech.model.Loan;
import com.agritech.model.LoanAffordability;
import com.agritech.model.MandiPrice;
import com.agritech.model.ProfitEstimate;
import com.agritech.model.SmartRecommendation;
import com.agritech.model.SowingAdvisory;
import com.agritech.model.WeatherReport;
import com.agritech.model.crop.Crop;
import com.agritech.model.crop.Maize;
import com.agritech.model.crop.Mustard;
import com.agritech.model.crop.Rice;
import com.agritech.model.crop.Wheat;
import com.agritech.model.equipment.FarmEquipment;
import com.agritech.narrator.AdvisoryNarrator;
import com.agritech.narrator.GeminiAdvisoryNarrator;
import com.agritech.narrator.LocalAdvisoryNarrator;
import com.agritech.repository.JdbcFarmerRepository;
import com.agritech.repository.JdbcEquipmentRepository;
import com.agritech.service.DataGovMandiService;
import com.agritech.service.CedaMandiService;
import com.agritech.service.EquipmentService;
import com.agritech.service.FarmerService;
import com.agritech.service.LoanService;
import com.agritech.service.LocationService;
import com.agritech.service.MandiService;
import com.agritech.service.OpenMeteoGeocodingService;
import com.agritech.service.OpenMeteoWeatherService;
import com.agritech.service.AgmarknetMandiService;
import com.agritech.service.ResilientMandiService;
import com.agritech.service.ProfitService;
import com.agritech.service.SmartFarmAdvisorService;
import com.agritech.service.WeatherService;
import com.agritech.util.ConsoleUI;
import com.agritech.util.InputUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner IN = new Scanner(System.in);
    private static final List<Crop> CROPS = List.of(new Wheat(), new Rice(), new Maize(), new Mustard());
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static FarmerService farmers;
    private static WeatherService weather;
    private static MandiService mandi;
    private static EquipmentService equipment;
    private static ConfigLoader config;
    private static DatabaseManager database;
    private static AdvisoryNarrator narrator;

    public static void main(String[] args) {
        try {
            initialize();
        } catch (Exception e) {
            ConsoleUI.banner();
            ConsoleUI.error(e.getMessage() == null ? "Application initialize nahi ho payi." : e.getMessage());
            ConsoleUI.info("MySQL Server start kijiye, MySQL Workbench me sql/01_agritech_schema.sql run kijiye, phir .\\setup.ps1 check kijiye.");
            return;
        }

        while (true) {
            menu();
            try {
                String choice = IN.nextLine().trim();
                switch (choice) {
                    case "1" -> completeAdvice();
                    case "2" -> selectedCrop();
                    case "3" -> weatherAndSowing();
                    case "4" -> marketAndProfit();
                    case "5" -> equipment();
                    case "6" -> loan();
                    case "7" -> profile();
                    case "8" -> status();
                    case "0" -> {
                        ConsoleUI.success("AgriTech Intelligence use karne ke liye thank you. Farm safely!");
                        return;
                    }
                    default -> ConsoleUI.warning("Menu me diya hua option choose kijiye.");
                }
            } catch (Exception e) {
                ConsoleUI.error(e.getMessage() == null ? "Request complete nahi ho payi." : e.getMessage());
                ConsoleUI.info("Main menu se dobara try kar sakte hain.");
            }
        }
    }

    private static void initialize() {
        config = new ConfigLoader();

        database = new DatabaseManager();
        database.verifyReady();
        farmers = new FarmerService(new JdbcFarmerRepository(database));
        equipment = new EquipmentService(new JdbcEquipmentRepository(database));

        LocationService locationService = new OpenMeteoGeocodingService();
        weather = new OpenMeteoWeatherService(locationService);
        mandi = new ResilientMandiService(
                new AgmarknetMandiService(),
                new CedaMandiService(),
                new DataGovMandiService(config.get("DATA_GOV_API_KEY", "data.gov.api.key"))
        );

        String geminiKey = config.get("GEMINI_API_KEY", "gemini.api.key");
        String geminiModel = config.get("GEMINI_MODEL", "gemini.model");
        narrator = geminiKey == null
                ? new LocalAdvisoryNarrator()
                : new GeminiAdvisoryNarrator(geminiKey, geminiModel);
    }

    private static void menu() {
        ConsoleUI.banner();
        ConsoleUI.menuItem("1", "Get Complete Smart Farm Advice");
        ConsoleUI.menuItem("2", "Check My Selected Crop");
        ConsoleUI.menuItem("3", "Weather & Best Sowing Time");
        ConsoleUI.menuItem("4", "Mandi Price & Profit Estimate");
        ConsoleUI.menuItem("5", "Equipment Rental");
        ConsoleUI.menuItem("6", "Loan / EMI Planner");
        ConsoleUI.menuItem("7", "Farmer Profile");
        ConsoleUI.menuItem("8", "Live Data / API Status");
        ConsoleUI.menuItem("0", "Exit");
        System.out.println();
        ConsoleUI.prompt("Choose an option: ");
    }

    private static Farmer farmer() {
        List<Farmer> all = farmers.getAll();
        if (all.isEmpty()) {
            ConsoleUI.info("Pehle farm profile create karte hain.");
            return register();
        }

        ConsoleUI.info("Registered farmer IDs:");
        all.stream().limit(8).forEach(f ->
                System.out.println("  " + ConsoleUI.yellow(f.getFarmerId()) + " - " + f.getName()));
        ConsoleUI.prompt("Farmer ID: ");
        return farmers.getById(IN.nextLine().trim());
    }

    private static Farmer register() {
        ConsoleUI.title("CREATE FARMER PROFILE");
        ConsoleUI.prompt("Name: ");
        String name = IN.nextLine();
        ConsoleUI.prompt("Phone (10 digits): ");
        String phone = IN.nextLine();
        ConsoleUI.prompt("Village: ");
        String village = IN.nextLine();
        ConsoleUI.prompt("District: ");
        String district = IN.nextLine();
        ConsoleUI.prompt("State: ");
        String state = IN.nextLine();
        double area = InputUtil.readDouble(IN, "Land area in acres: ");
        SoilType soil = soil();
        boolean irrigation = InputUtil.readYesNo(IN, "Irrigation available?");

        Farmer farmer = farmers.register(name, village, district, state, area, soil, phone, irrigation);
        ConsoleUI.success("Farm profile ready hai. Farmer ID: " + farmer.getFarmerId());
        return farmer;
    }

    private static void updateProfile() {
        Farmer current = farmer();
        ConsoleUI.title("UPDATE FARMER PROFILE");
        ConsoleUI.info("Current details ke basis par new values enter kijiye.");

        ConsoleUI.prompt("Name [" + current.getName() + "]: ");
        String name = keep(IN.nextLine(), current.getName());
        ConsoleUI.prompt("Phone [" + current.getPhone() + "]: ");
        String phone = keep(IN.nextLine(), current.getPhone());
        ConsoleUI.prompt("Village [" + current.getVillage() + "]: ");
        String village = keep(IN.nextLine(), current.getVillage());
        ConsoleUI.prompt("District [" + current.getDistrict() + "]: ");
        String district = keep(IN.nextLine(), current.getDistrict());
        ConsoleUI.prompt("State [" + current.getState() + "]: ");
        String state = keep(IN.nextLine(), current.getState());

        ConsoleUI.prompt("Land area in acres [" + current.getLandArea() + "]: ");
        String areaText = IN.nextLine().trim();
        double area = areaText.isBlank() ? current.getLandArea() : Double.parseDouble(areaText);

        ConsoleUI.prompt("Change soil type? (y/n): ");
        boolean changeSoil = IN.nextLine().trim().equalsIgnoreCase("y");
        SoilType soil = changeSoil ? soil() : current.getSoilType();

        boolean irrigation = InputUtil.readYesNo(IN,
                "Irrigation available? Current=" + (current.isIrrigationAvailable() ? "yes" : "no"));

        Farmer updated = farmers.update(current.getFarmerId(), name, village, district, state,
                area, soil, phone, irrigation);
        ConsoleUI.success("Profile update ho gaya.");
        System.out.println(updated);
    }

    private static String keep(String entered, String current) {
        return entered == null || entered.isBlank() ? current : entered.trim();
    }

    private static SoilType soil() {
        ConsoleUI.info("Choose soil type:");
        System.out.println("1. Loamy");
        System.out.println("2. Clay");
        System.out.println("3. Sandy");
        System.out.println("4. Sandy Loam");
        System.out.println("5. Clay Loam");
        int choice = InputUtil.readInt(IN, "Choice: ");
        if (choice < 1 || choice > 5) throw new IllegalArgumentException("Soil option 1 se 5 choose kijiye.");
        return SoilType.values()[choice - 1];
    }

    private static Crop crop() {
        ConsoleUI.info("Choose crop:");
        for (int i = 0; i < CROPS.size(); i++) {
            System.out.println((i + 1) + ". " + CROPS.get(i).getName());
        }
        int choice = InputUtil.readInt(IN, "Choice: ");
        if (choice < 1 || choice > CROPS.size()) throw new IllegalArgumentException("Listed crop choose kijiye.");
        return CROPS.get(choice - 1);
    }

    private static WeatherReport report(Farmer farmer) {
        WeatherReport report = weather.getReport(farmer.getVillage(), farmer.getDistrict(), farmer.getState());
        ConsoleUI.title("LIVE WEATHER DATA");
        ConsoleUI.label("Resolved Location", report.location());
        ConsoleUI.label("Source", report.source());
        ConsoleUI.label("Retrieved", formatInstant(report.retrievedAt())
                + (report.cached() ? " | CACHED REAL DATA" : " | LIVE FETCH"));
        return report;
    }

    private static void completeAdvice() {
        Farmer farmer = farmer();
        WeatherReport weatherReport = report(farmer);

        ConsoleUI.info("Farm analyze ho raha hai: soil + weather + water + mandi + profit + safety...");
        List<SmartRecommendation> recommendations = new SmartFarmAdvisorService(mandi, new ProfitService())
                .recommend(farmer, CROPS, weatherReport.days());

        ConsoleUI.title("YOUR SMART FARM ADVISORY");
        ConsoleUI.label("Farmer", farmer.getName());
        ConsoleUI.label("Farm", String.format("%.2f acres | Soil: %s", farmer.getLandArea(), farmer.getSoilType()));
        ConsoleUI.label("Location", farmer.getDistrict() + ", " + farmer.getState());

        if (recommendations.isEmpty()) {
            ConsoleUI.warning("Currently supported crops (Wheat, Rice, Maize, Mustard) me koi crop critical soil/weather safety gate pass nahi kar raha.");
            ConsoleUI.info("Ye poore India ke sabhi crops ka verdict nahi hai. Profile/soil sahi hai to local suitable crop guidance consider kijiye aur weather change hone par recheck kijiye.");
            return;
        }

        int rank = 1;
        for (SmartRecommendation recommendation : recommendations) {
            showRecommendation(rank++, recommendation);
        }

        ConsoleUI.title("NEXT ACTIONS");
        System.out.println("1. Field work se pehle weather forecast recheck kijiye.");
        System.out.println("2. Seed aur irrigation availability confirm kijiye.");
        System.out.println("3. Sale plan se pehle mandi data date check kijiye.");
        ConsoleUI.info("Profit values estimates hain, guaranteed return nahi.");
    }

    private static void showRecommendation(int rank, SmartRecommendation r) {
        System.out.println();
        String scoreType = r.mandi() == null ? "Agronomic Score" : "Overall Score";
        System.out.println(ConsoleUI.bold(rank + ". " + r.crop().getName()));
        ConsoleUI.label(scoreType, ConsoleUI.scoreBar(r.score()) + " | " + r.label());
        ConsoleUI.label("Risk", ConsoleUI.risk(r.risk()));
        System.out.println("  Soil   " + ConsoleUI.scoreBar(r.soilScore()));
        System.out.println("  Weather" + ConsoleUI.scoreBar(r.weatherScore()));
        System.out.println("  Water  " + ConsoleUI.scoreBar(r.waterScore()));
        System.out.println("  Sowing " + ConsoleUI.scoreBar(r.sowing().suitabilityScore()));
        System.out.println();
        System.out.println(narrator.narrate(r));
        showMarket(r.mandi());
        showEconomics(r.economics());
    }

    private static void selectedCrop() {
        Farmer farmer = farmer();
        Crop crop = crop();
        WeatherReport weatherReport = report(farmer);
        List<SmartRecommendation> all = new SmartFarmAdvisorService(mandi, new ProfitService())
                .recommend(farmer, List.of(crop), weatherReport.days());

        ConsoleUI.title("SELECTED CROP CHECK");
        if (all.isEmpty()) {
            ConsoleUI.error("NOT RECOMMENDED");
            if (!crop.supportsSoil(farmer.getSoilType())) {
                ConsoleUI.warning("Aapki " + farmer.getSoilType() + " soil is crop ke verified supported-soil list me nahi hai.");
            }
            ConsoleUI.info("Current soil ya weather critical safety gate pass nahi kar raha. Fake market/profit score add nahi kiya gaya.");
            return;
        }

        SmartRecommendation r = all.get(0);
        String choice = r.score() >= 70 ? "GOOD CHOICE"
                : r.score() >= 55 ? "POSSIBLE WITH CAUTION"
                : "NOT RECOMMENDED";
        ConsoleUI.label("Decision", ConsoleUI.status(choice));
        showRecommendation(1, r);
    }

    private static void weatherAndSowing() {
        Farmer farmer = farmer();
        Crop crop = crop();
        WeatherReport weatherReport = report(farmer);

        ConsoleUI.title("7-DAY WEATHER FORECAST");
        System.out.printf("%-12s %-13s %-12s %-12s %-12s%n",
                "Date", "Temp C", "Rain", "Humidity", "Wind");
        for (DailyForecast day : weatherReport.days()) {
            System.out.printf("%-12s %5.1f-%-5.1f %5.1fmm/%-3.0f%% %6.0f%%      %5.1fkm/h%n",
                    day.date(), day.minTemperature(), day.maxTemperature(), day.rainMm(),
                    day.rainProbability(), day.humidity(), day.windSpeed());
        }

        SowingAdvisory advice = new SowingDecisionEngine().advise(crop, weatherReport.days());
        ConsoleUI.title("SMART SOWING ADVICE");
        ConsoleUI.label("Crop", crop.getName());
        ConsoleUI.label("Status", ConsoleUI.status(advice.status()));
        ConsoleUI.label("Risk", ConsoleUI.risk(advice.riskLevel()));
        ConsoleUI.label("Suitability", ConsoleUI.scoreBar(advice.suitabilityScore()));
        ConsoleUI.label("Recommended Window",
                advice.recommendedStart() == null
                        ? "Current 7-day forecast me safe window nahi mila"
                        : advice.recommendedStart() + " to " + advice.recommendedEnd());
        ConsoleUI.label("Kyun", advice.explanation());
        ConsoleUI.label("Ab kya karein", advice.farmerAction());
    }

    private static void marketAndProfit() {
        Farmer farmer = farmer();
        Crop crop = crop();
        ConsoleUI.title("MANDI PRICE & PROFIT ESTIMATE");

        MandiPrice price;
        try {
            price = mandi.bestPrice(crop.getName(), farmer.getState(), farmer.getDistrict());
        } catch (ApiException e) {
            ConsoleUI.warning("Live mandi sources abhi response nahi de rahe.");
            ConsoleUI.info("App ne AGMARKNET -> CEDA -> data.gov.in tino genuine sources try kiye.");
            ConsoleUI.info("Koi fake price use nahi hua.");

            boolean manual = InputUtil.readYesNo(IN,
                    "Kya aap latest VERIFIED mandi price manually enter karke profit estimate continue karna chahte hain?");
            if (!manual) {
                ConsoleUI.info("Weather, sowing aur crop advisory bina fake market data ke continue kar sakte hain.");
                return;
            }

            double manualPrice = InputUtil.readDouble(IN, "Verified modal mandi price / quintal (Rs): ");
            ConsoleUI.prompt("Market/source name: ");
            String manualMarket = IN.nextLine().trim();
            if (manualMarket.isBlank()) manualMarket = "User-provided verified market price";

            price = new MandiPrice(
                    crop.getName(),
                    farmer.getState(),
                    farmer.getDistrict(),
                    manualMarket,
                    manualPrice,
                    manualPrice,
                    manualPrice,
                    0,
                    java.time.LocalDate.now(),
                    "",
                    "User-provided verified market price",
                    true,
                    false,
                    java.time.Instant.now()
            );
        }

        showMarket(price);
        double expectedYield = InputUtil.readDouble(IN, "Expected yield per acre (quintals): ");
        double costPerAcre = InputUtil.readDouble(IN, "Estimated cultivation cost per acre (Rs): ");
        ProfitEstimate estimate = new ProfitService().calculate(
                farmer.getLandArea(), expectedYield,
                BigDecimal.valueOf(price.modalPrice()), BigDecimal.valueOf(costPerAcre)
        );
        showEconomics(estimate);
    }

    private static void showMarket(MandiPrice price) {
        ConsoleUI.title("MARKET DATA");
        if (price == null) {
            ConsoleUI.warning("Genuine current mandi price available nahi hua; market/profit live evidence include nahi hua.");
            return;
        }

        ConsoleUI.label("Commodity", price.cropName());
        ConsoleUI.label("Market", price.mandiName() + ", " + price.district());
        ConsoleUI.money("Min Price / quintal", String.format("%.2f", price.minPrice()));
        ConsoleUI.money("Max Price / quintal", String.format("%.2f", price.maxPrice()));
        ConsoleUI.money("Modal Price / quintal", String.format("%.2f", price.modalPrice()));
        ConsoleUI.label("Data Date", price.priceDate());
        ConsoleUI.label("Source", price.source());
        if (!price.exactDistrict()) ConsoleUI.info("Exact district record nahi mila; latest available state-level genuine result dikhaya gaya hai.");
        if (price.cached()) ConsoleUI.info("Ye previously fetched REAL cached mandi data hai. Retrieved: " + formatInstant(price.retrievedAt()));
        if (price.source() != null && price.source().contains("CEDA")) {
            ConsoleUI.info("CEDA fallback latest available DMI/AGMARKNET-derived data dikhata hai; Data Date zaroor check kijiye.");
        }
    }

    private static void showEconomics(ProfitEstimate estimate) {
        ConsoleUI.title("ESTIMATED ECONOMICS");
        if (estimate == null) {
            ConsoleUI.warning("Mandi/economic evidence unavailable hai.");
            return;
        }
        ConsoleUI.money("Estimated Revenue", estimate.revenue());
        ConsoleUI.money("Estimated Cost", estimate.cost());
        ConsoleUI.money("Estimated Profit", estimate.profit());
        ConsoleUI.money("Estimated Profit / acre", estimate.profitPerAcre());
        ConsoleUI.info("Ye planning estimate hai, guaranteed market return nahi.");
    }

    private static void equipment() {
        ConsoleUI.title("EQUIPMENT RENTAL");
        System.out.println("1. View inventory");
        System.out.println("2. Add inventory");
        System.out.println("3. Book equipment");
        System.out.println("4. Return equipment");
        int choice = InputUtil.readInt(IN, "Choice: ");

        switch (choice) {
            case 1 -> {
                if (equipment.getAll().isEmpty()) {
                    ConsoleUI.info("Abhi koi equipment inventory add nahi hui. Option 2 se real inventory add kijiye.");
                }
                for (FarmEquipment e : equipment.getAll()) {
                    System.out.printf("%s | %-18s | Rs %.2f/day | %d available%n",
                            e.getId(), e.getName(), e.getDailyRate(), e.getAvailableUnits());
                }
            }
            case 2 -> {
                ConsoleUI.prompt("ID: ");
                String id = IN.nextLine();
                ConsoleUI.prompt("Type [Tractor/Harvester/Irrigation Pump/Rotavator]: ");
                String type = IN.nextLine();
                double rate = InputUtil.readDouble(IN, "Rate/day: ");
                int quantity = InputUtil.readInt(IN, "Quantity: ");
                equipment.add(id, type, rate, quantity);
                ConsoleUI.success("Equipment rental inventory me add ho gaya.");
            }
            case 3 -> {
                ConsoleUI.prompt("Equipment ID: ");
                String id = IN.nextLine();
                int days = InputUtil.readInt(IN, "Rental days: ");
                ConsoleUI.money("Booking Total", String.format("%.2f", equipment.book(id, days)));
                ConsoleUI.success("Booking record ho gayi aur availability update ho gayi.");
            }
            case 4 -> {
                ConsoleUI.prompt("Equipment ID: ");
                String id = IN.nextLine();
                equipment.returnOne(id);
                ConsoleUI.success("Equipment return record ho gaya.");
            }
            default -> throw new IllegalArgumentException("Equipment option 1 se 4 choose kijiye.");
        }
    }

    private static void loan() {
        ConsoleUI.title("LOAN / EMI PLANNER");
        System.out.println("1. EMI Calculator");
        System.out.println("2. Loan Affordability Planner");
        int choice = InputUtil.readInt(IN, "Choice: ");
        LoanService service = new LoanService();

        if (choice == 1) {
            double principal = InputUtil.readDouble(IN, "Loan amount (Rs): ");
            double rate = InputUtil.readDouble(IN, "Annual interest rate (%): ");
            int months = InputUtil.readInt(IN, "Tenure (months): ");
            Loan loan = service.create("Planning estimate", principal, rate, months);

            ConsoleUI.title("EMI RESULT");
            ConsoleUI.money("Monthly EMI", String.format("%.2f", loan.calculateEmi()));
            ConsoleUI.money("Total Payable", String.format("%.2f", loan.totalPayable()));
            ConsoleUI.money("Total Interest", String.format("%.2f", loan.totalInterest()));
            ConsoleUI.info("Ye financial planning estimate hai, bank approval nahi.");
            return;
        }

        if (choice == 2) {
            double farmIncome = InputUtil.readDouble(IN, "Average monthly farm income (Rs): ");
            double otherIncome = InputUtil.readDouble(IN, "Other monthly income (Rs): ");
            double existingEmi = InputUtil.readDouble(IN, "Existing monthly EMI (Rs): ");
            double requestedLoan = InputUtil.readDouble(IN, "Requested loan amount (Rs): ");
            double rate = InputUtil.readDouble(IN, "Annual interest rate (%): ");
            int months = InputUtil.readInt(IN, "Tenure (months): ");

            LoanAffordability a = service.assessAffordability(
                    farmIncome, otherIncome, existingEmi, requestedLoan, rate, months
            );

            ConsoleUI.title("LOAN AFFORDABILITY ADVISORY");
            ConsoleUI.money("Monthly Income", String.format("%.2f", a.monthlyIncome()));
            ConsoleUI.money("Existing EMI", String.format("%.2f", a.existingEmi()));
            ConsoleUI.money("Estimated Safe New EMI", String.format("%.2f", a.safeNewEmi()));
            ConsoleUI.money("Requested Loan EMI", String.format("%.2f", a.requestedLoanEmi()));
            ConsoleUI.money("Estimated Max Affordable Loan", String.format("%.2f", a.estimatedMaxAffordableLoan()));
            ConsoleUI.label("Financial Pressure", ConsoleUI.status(a.status()));
            ConsoleUI.info(a.note());
            return;
        }

        throw new IllegalArgumentException("Loan option 1 ya 2 choose kijiye.");
    }

    private static void profile() {
        ConsoleUI.title("FARMER PROFILE");
        System.out.println("1. View profile");
        System.out.println("2. Register new farmer");
        System.out.println("3. Update existing profile");
        int choice = InputUtil.readInt(IN, "Choice: ");
        switch (choice) {
            case 1 -> {
                Farmer f = farmer();
                ConsoleUI.title("PROFILE DETAILS");
                System.out.println(f);
            }
            case 2 -> register();
            case 3 -> updateProfile();
            default -> throw new IllegalArgumentException("Profile option 1 se 3 choose kijiye.");
        }
    }

    private static void status() {
        ConsoleUI.title("LIVE DATA / API STATUS");
        ConsoleUI.label("Database",
                database.isReachable()
                        ? ConsoleUI.status("CONNECTED") + " | MySQL via JDBC | " + database.displayTarget()
                        : ConsoleUI.status("OFFLINE") + " | MySQL/JDBC connection failed");
        ConsoleUI.label("Weather Service", ConsoleUI.status("READY") + " | Open-Meteo | no key required");
        ConsoleUI.label("Location Service", ConsoleUI.status("READY") + " | Open-Meteo Geocoding | strict state matching");
        boolean dataGovConfigured = config.configured("DATA_GOV_API_KEY", "data.gov.api.key");
        ConsoleUI.label("Mandi Service",
                ConsoleUI.status("READY")
                        + " | AGMARKNET 2.0 -> CEDA -> data.gov.in"
                        + (dataGovConfigured ? " | data.gov.in key configured" : " | no key needed for first two sources")
                        + " | in-memory real cache");
        ConsoleUI.label("AI Narrator",
                config.configured("GEMINI_API_KEY", "gemini.api.key")
                        ? ConsoleUI.status("CONFIGURED") + " | Gemini Hinglish + safe local fallback"
                        : ConsoleUI.status("LOCAL MODE") + " | local Hinglish narrator");
        ConsoleUI.info("Secret keys kabhi display nahi ki jati.");
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) return "Unknown";
        return TIME_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
    }
}
