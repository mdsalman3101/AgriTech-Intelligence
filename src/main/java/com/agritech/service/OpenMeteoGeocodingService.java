package com.agritech.service;

import com.agritech.exception.ApiException;
import com.agritech.infrastructure.HttpClientProvider;
import com.agritech.infrastructure.Json;
import com.agritech.model.LocationResult;
import com.agritech.util.LocationNormalizer;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OpenMeteoGeocodingService implements LocationService {
    private HttpClient client;

    public OpenMeteoGeocodingService() {
        this(null);
    }

    public OpenMeteoGeocodingService(HttpClient client) {
        this.client = client;
    }

    private HttpClient http() {
        if (client == null) client = HttpClientProvider.create();
        return client;
    }

    @Override
    public LocationResult resolve(String village, String district, String state) {
        String cleanVillage = LocationNormalizer.clean(village);
        String cleanDistrict = LocationNormalizer.clean(district);
        String cleanState = LocationNormalizer.normalizeStateForStorage(state);

        if (cleanVillage.isBlank() && cleanDistrict.isBlank()) {
            throw new ApiException("Live weather ke liye village ya district required hai.");
        }

        ApiException last = null;

        if (!cleanVillage.isBlank()) {
            for (String variant : variants(cleanVillage)) {
                try {
                    return fetch(variant, cleanDistrict, cleanState, true);
                } catch (ApiException e) {
                    last = e;
                }
            }
        }

        if (!cleanDistrict.isBlank()) {
            for (String variant : variants(cleanDistrict)) {
                try {
                    return fetch(variant, cleanDistrict, cleanState, false);
                } catch (ApiException e) {
                    last = e;
                }
            }
        }

        throw new ApiException(
                "Location " + cleanState + " ke andar safely resolve nahi ho payi. "
                        + "Village/district spelling check kijiye.",
                last
        );
    }

    private LocationResult fetch(String searchName, String district, String state, boolean villageSearch) {
        try {
            String encoded = URLEncoder.encode(searchName, StandardCharsets.UTF_8);
            URI uri = URI.create(
                    "https://geocoding-api.open-meteo.com/v1/search"
                            + "?name=" + encoded
                            + "&count=100"
                            + "&language=en"
                            + "&format=json"
                            + "&countryCode=IN"
            );

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = http().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ApiException("Location service HTTP " + response.statusCode() + " return kar raha hai.");
            }

            return parseForRuntime(response.body(), searchName, district, state, villageSearch);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Location service reach nahi ho pa rahi. Internet aur place name check kijiye.", e);
        }
    }

    /**
     * Simple parser kept for deterministic unit tests.
     */
    public LocationResult parse(String body) {
        try {
            JsonNode results = Json.MAPPER.readTree(body).path("results");
            if (!results.isArray() || results.isEmpty()) {
                throw new ApiException("Location response empty hai.");
            }
            return toLocation(results.get(0));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Location response read nahi ho paya.", e);
        }
    }

    /**
     * Public strict parser used by tests to verify state safety.
     */
    public LocationResult parse(String body, String district, String state) {
        return parseForRuntime(body, district, district, state, false);
    }

    private LocationResult parseForRuntime(String body, String searchedName,
                                           String district, String state,
                                           boolean villageSearch) {
        try {
            JsonNode results = Json.MAPPER.readTree(body).path("results");
            if (!results.isArray() || results.isEmpty()) {
                throw new ApiException("Open-Meteo me location nahi mili.");
            }

            JsonNode best = null;
            int bestScore = Integer.MIN_VALUE;

            for (JsonNode node : results) {
                String countryCode = node.path("country_code").asText("");
                if (!countryCode.isBlank() && !countryCode.equalsIgnoreCase("IN")) continue;

                String name = node.path("name").asText("");
                String admin1 = node.path("admin1").asText("");
                String admin2 = node.path("admin2").asText("");

                if (!state.isBlank() && !LocationNormalizer.samePlace(admin1, state)) {
                    continue;
                }

                if (villageSearch && !district.isBlank() && !admin2.isBlank()
                        && !LocationNormalizer.samePlace(admin2, district)) {
                    continue;
                }

                int score = 0;
                if (LocationNormalizer.samePlace(name, searchedName)) score += 50;
                if (!state.isBlank() && LocationNormalizer.samePlace(admin1, state)) score += 30;
                if (!district.isBlank()) {
                    if (LocationNormalizer.samePlace(admin2, district)) score += 30;
                    if (LocationNormalizer.samePlace(name, district)) score += 25;
                }

                if (score > bestScore) {
                    best = node;
                    bestScore = score;
                }
            }

            if (best == null) {
                throw new ApiException(
                        "Similar place mila, lekin farmer ke state/district se match nahi hua."
                );
            }
            return toLocation(best);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Location response read nahi ho paya.", e);
        }
    }

    private LocationResult toLocation(JsonNode node) {
        return new LocationResult(
                node.path("latitude").asDouble(),
                node.path("longitude").asDouble(),
                LocationNormalizer.ascii(node.path("name").asText()),
                LocationNormalizer.ascii(node.path("admin1").asText()),
                LocationNormalizer.ascii(node.path("country").asText())
        );
    }

    private List<String> variants(String place) {
        Set<String> values = new LinkedHashSet<>();
        String cleaned = LocationNormalizer.clean(place);
        if (cleaned.isBlank()) return new ArrayList<>();

        values.add(cleaned);
        String withoutSuffix = cleaned
                .replaceAll("(?i)\\s+(district|city|taluka|tehsil)$", "")
                .trim();
        if (!withoutSuffix.isBlank()) values.add(withoutSuffix);

        String key = LocationNormalizer.key(withoutSuffix);
        if (key.equals("mehsana")) values.add("Mahesana");
        if (key.equals("mahesana")) values.add("Mehsana");
        if (key.equals("ahmedabad")) values.add("Ahmadabad");
        if (key.equals("ahmadabad")) values.add("Ahmedabad");

        return new ArrayList<>(values);
    }
}
