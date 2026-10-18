package com.agritech.service;

import com.agritech.exception.ApiException;
import com.agritech.infrastructure.HttpClientProvider;
import com.agritech.infrastructure.Json;
import com.agritech.model.DailyForecast;
import com.agritech.model.LocationResult;
import com.agritech.model.WeatherReport;
import com.agritech.util.LocationNormalizer;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenMeteoWeatherService implements WeatherService {
    private static final Duration FRESH_CACHE = Duration.ofMinutes(15);
    private static final Duration STALE_CACHE = Duration.ofHours(24);

    private final LocationService locations;
    private HttpClient client;
    private final Map<String, WeatherReport> cache = new HashMap<>();

    public OpenMeteoWeatherService(LocationService locations) {
        this(locations, null);
    }

    public OpenMeteoWeatherService(LocationService locations, HttpClient client) {
        this.locations = locations;
        this.client = client;
    }

    private HttpClient http() {
        if (client == null) client = HttpClientProvider.create();
        return client;
    }

    @Override
    public List<DailyForecast> getSevenDayForecast(String district, String state) {
        return getReport("", district, state).days();
    }

    @Override
    public synchronized WeatherReport getReport(String village, String district, String state) {
        String cacheKey = LocationNormalizer.canonicalKey(village) + "|"
                + LocationNormalizer.canonicalKey(district) + "|"
                + LocationNormalizer.canonicalKey(state);

        WeatherReport previous = cache.get(cacheKey);
        if (previous != null && age(previous).compareTo(FRESH_CACHE) <= 0) {
            return new WeatherReport(previous.days(), previous.location(), previous.source(), previous.retrievedAt(), true);
        }

        try {
            LocationResult location = locations.resolve(village, district, state);
            String url = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + location.latitude()
                    + "&longitude=" + location.longitude()
                    + "&daily=temperature_2m_min,temperature_2m_max,precipitation_sum,"
                    + "precipitation_probability_max,relative_humidity_2m_mean,wind_speed_10m_max"
                    + "&timezone=auto&forecast_days=7";

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("User-Agent", "AgriTech-Intelligence/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = http().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ApiException("Weather service HTTP " + response.statusCode() + " return kar rahi hai.");
            }

            WeatherReport report = new WeatherReport(
                    parse(response.body()),
                    location.resolvedPlaceName() + ", " + location.region(),
                    "Open-Meteo",
                    Instant.now(),
                    false
            );
            cache.put(cacheKey, report);
            return report;
        } catch (Exception e) {
            if (previous != null && age(previous).compareTo(STALE_CACHE) <= 0) {
                return new WeatherReport(previous.days(), previous.location(), previous.source(), previous.retrievedAt(), true);
            }
            if (e instanceof ApiException api) throw api;
            throw new ApiException("Latest weather unavailable hai aur real cached forecast bhi nahi mila.", e);
        }
    }

    public List<DailyForecast> parse(String body) {
        try {
            JsonNode daily = Json.MAPPER.readTree(body).path("daily");
            JsonNode times = daily.path("time");
            if (!times.isArray() || times.isEmpty()) {
                throw new ApiException("Weather response empty hai.");
            }

            List<DailyForecast> out = new ArrayList<>();
            for (int i = 0; i < times.size(); i++) {
                out.add(new DailyForecast(
                        LocalDate.parse(times.get(i).asText()),
                        at(daily, "temperature_2m_min", i),
                        at(daily, "temperature_2m_max", i),
                        at(daily, "precipitation_sum", i),
                        at(daily, "precipitation_probability_max", i),
                        at(daily, "relative_humidity_2m_mean", i),
                        at(daily, "wind_speed_10m_max", i)
                ));
            }
            return out;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Weather response read nahi ho paya.", e);
        }
    }

    private double at(JsonNode daily, String key, int index) {
        JsonNode array = daily.path(key);
        return array.isArray() && index < array.size() && !array.get(index).isNull()
                ? array.get(index).asDouble()
                : 0.0;
    }

    private Duration age(WeatherReport report) {
        return Duration.between(report.retrievedAt(), Instant.now()).abs();
    }
}
