# 3-Member Java Backend Division

## MEMBER 1 — TEAM LEAD
Own:
- Farmer.java
- CropRecommendation.java
- all `model/crop/*`
- FarmerRepository.java
- FileFarmerRepository.java
- FarmerService.java
- CropCatalogService.java
- CropRecommendationEngine.java
- Main.java integration

Target:
Farmer -> profile -> crops -> weather/market data -> Top 3 crop recommendations.

Suggested branches:
- feature/farmer-module
- feature/crop-catalog
- feature/crop-recommendation
- feature/main-integration

---

## MEMBER 2 — WEATHER & SOWING
Own:
- DailyForecast.java
- SowingAdvisory.java
- WeatherService.java
- DemoWeatherService.java
- SowingDecisionEngine.java

Target:
District/state -> 7-day demo forecast -> selected crop -> sowing window + reason + risk.

Work order:
1. DailyForecast
2. WeatherService
3. DemoWeatherService
4. Print 7-day forecast
5. SowingAdvisory
6. SowingDecisionEngine

Suggested branches:
- feature/weather-core
- feature/sowing-advisor

---

## MEMBER 3 — PROFIT, MANDI, EQUIPMENT, LOAN
Own:
- MandiPrice.java
- Loan.java
- all `model/equipment/*`
- MandiService.java
- ProfitService.java
- EquipmentService.java
- LoanService.java

Target:
- Crop -> mandi price -> cost/revenue/profit
- Equipment -> availability -> booking -> cost
- Loan -> amount/rate/tenure -> EMI

Suggested branches:
- feature/mandi-module
- feature/profit-module
- feature/equipment-rental
- feature/loan-module

---

## SHARED
Team Lead controls final merge:
- enums/*
- data/DemoData.java
- exception/*
- util/*

## Rule for weaker members
Only one class/task at a time:
1. Understand class
2. Implement one method
3. Run terminal
4. Commit
5. Push
6. PR to dev
