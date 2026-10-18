# Member 2 - Weather and Smart Sowing Module

## My Responsibility
I worked on the Weather and Smart Sowing part of AgriTech Intelligence.

## Main Components
- Open-Meteo weather integration
- Location/geocoding flow
- 7-day weather forecast
- DailyForecast model
- SowingDecisionEngine
- Sowing risk classification
- Recommended sowing window

## How It Works
Farmer location is resolved first.
Real weather data is fetched using Open-Meteo.
The Java SowingDecisionEngine checks:
- crop temperature range
- rainfall
- rain probability
- humidity
- wind
- water requirement

Then the system returns:
- RECOMMENDED / WAIT / NOT RECOMMENDED
- suitability score
- LOW / MEDIUM / HIGH risk
- recommended sowing window
- clear reason

## Important
The weather API only provides data.
The final sowing decision is calculated by Java logic.
