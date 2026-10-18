# Team Work Division

This file records the intended ownership of the final integrated codebase.

## Member 1 / Team Lead — Farmer + Crop Intelligence + Integration
- Farmer model/service/repository
- Crop hierarchy/catalogue
- SmartFarmAdvisorService integration
- Main terminal application and final integration

## Member 2 — Weather + Smart Sowing
- DailyForecast / WeatherReport
- WeatherService
- OpenMeteoGeocodingService
- OpenMeteoWeatherService
- SowingDecisionEngine / SowingAdvisory

## Member 3 — Market + Profit + Equipment + Loan
- DataGovMandiService / MandiPrice
- ProfitService / ProfitEstimate
- Equipment hierarchy/service/persistence
- Loan / EMI planner

Git workflow: feature branch -> meaningful commits -> pull request to `dev` -> review/test -> merge.
