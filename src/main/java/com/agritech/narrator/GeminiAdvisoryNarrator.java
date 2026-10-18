package com.agritech.narrator;

import com.agritech.infrastructure.HttpClientProvider;
import com.agritech.infrastructure.Json;
import com.agritech.model.MandiPrice;
import com.agritech.model.ProfitEstimate;
import com.agritech.model.SmartRecommendation;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class GeminiAdvisoryNarrator implements AdvisoryNarrator {
    private final String apiKey;
    private final String model;
    private final HttpClient client;
    private final AdvisoryNarrator fallback = new LocalAdvisoryNarrator();

    public GeminiAdvisoryNarrator(String apiKey, String model) {
        this(apiKey, model, HttpClientProvider.create());
    }

    GeminiAdvisoryNarrator(String apiKey, String model, HttpClient client) {
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? "gemini-3.5-flash-lite" : model.trim();
        this.client = client;
    }

    @Override
    public String narrate(SmartRecommendation recommendation) {
        if (apiKey == null || apiKey.isBlank()) return fallback.narrate(recommendation);

        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", buildPrompt(recommendation)))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "maxOutputTokens", 450
                    )
            );

            String json = Json.MAPPER.writeValueAsString(body);
            URI uri = URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent"
            );

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return fallback.narrate(recommendation);

            JsonNode candidates = Json.MAPPER.readTree(response.body()).path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) return fallback.narrate(recommendation);

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) return fallback.narrate(recommendation);

            String text = parts.get(0).path("text").asText("").trim();
            return text.isBlank() ? fallback.narrate(recommendation) : text;
        } catch (Exception ignored) {
            return fallback.narrate(recommendation);
        }
    }

    private String buildPrompt(SmartRecommendation r) {
        String market = "unavailable";
        MandiPrice m = r.mandi();
        if (m != null) {
            market = "modal price Rs " + m.modalPrice() + "/quintal, market " + m.mandiName()
                    + ", district " + m.district() + ", data date " + m.priceDate()
                    + ", cached real data " + m.cached();
        }

        String economics = "unavailable";
        ProfitEstimate p = r.economics();
        if (p != null) {
            economics = "estimated revenue Rs " + p.revenue() + ", cost Rs " + p.cost()
                    + ", profit Rs " + p.profit() + ", profit per acre Rs " + p.profitPerAcre();
        }

        return """
                You are a friendly formatter for an Indian farmer-facing Java project.

                LANGUAGE:
                - Write in simple natural Hinglish: Hindi meaning written in English/Roman script.
                - Do NOT use Devanagari.
                - Keep common farming words in English when natural: weather, soil, crop, market, mandi,
                  profit, risk, temperature, sowing, rainfall, irrigation.
                - Use short, clear sentences. No emojis. No markdown table.

                SAFETY / FACT RULES:
                - Do NOT invent, correct, add, or change any supplied fact, number, date, price,
                  weather value, score, risk, recommendation, or calculation.
                - If market/economics is unavailable, clearly say so.
                - AI only explains facts calculated by Java. It must never override Java decisions.

                FORMAT:
                Namaste!
                [one short recommendation summary]

                Kyun?
                - ...
                - ...

                Sowing Advice:
                ...

                Risk:
                ...

                Ab kya karein?
                ...

                Market/Profit Note:
                ...

                FACTS:
                Crop: %s
                Score: %.1f/100
                Label: %s
                Risk: %s
                Soil score: %.1f/100
                Weather score: %.1f/100
                Water score: %.1f/100
                Sowing status: %s
                Sowing score: %d/100
                Recommended start: %s
                Recommended end: %s
                Sowing reason: %s
                Farmer action: %s
                Market: %s
                Economics: %s
                Internal explanation: %s
                """.formatted(
                r.crop().getName(), r.score(), r.label(), r.risk(),
                r.soilScore(), r.weatherScore(), r.waterScore(),
                r.sowing().status(), r.sowing().suitabilityScore(),
                r.sowing().recommendedStart(), r.sowing().recommendedEnd(),
                r.sowing().explanation(), r.sowing().farmerAction(),
                market, economics, r.explanation()
        );
    }
}
