package com.agritech.service;

import com.agritech.exception.ApiException;
import com.agritech.infrastructure.HttpClientProvider;
import com.agritech.infrastructure.Json;
import com.agritech.model.MandiPrice;
import com.agritech.util.LocationNormalizer;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataGovMandiService implements MandiService {
    public static final String SOURCE = "data.gov.in / AGMARKNET";
    private static final String RESOURCE = "9ef84268-d588-465a-a308-a864a43d0070";
    private static final Duration FRESH_CACHE = Duration.ofHours(6);
    private static final Duration STALE_CACHE = Duration.ofDays(7);
    private static final int MAX_ATTEMPTS = 2;

    private final String key;
    private HttpClient client;
    private final Map<String, CacheEnvelope> memoryCache = new HashMap<>();
    private Instant lastLiveCall = Instant.EPOCH;
    private Instant rateLimitedUntil = Instant.EPOCH;

    public DataGovMandiService(String key) {
        this(key, null);
    }

    public DataGovMandiService(String key, HttpClient client) {
        this.key = key;
        this.client = client;
    }

    private HttpClient http() {
        if (client == null) client = HttpClientProvider.create();
        return client;
    }

    @Override
    public synchronized List<MandiPrice> findPrices(String crop, String state, String district) {
        if (key == null || key.isBlank()) {
            throw new ApiException(
                    "Mandi API key configured nahi hai. setup.ps1 run kijiye ya DATA_GOV_API_KEY set kijiye."
            );
        }

        String normalizedState = LocationNormalizer.normalizeStateForStorage(state);
        String normalizedDistrict = LocationNormalizer.normalizeDistrictForStorage(district);
        String cacheKey = cacheKey(crop, normalizedState, normalizedDistrict);

        CacheEnvelope cached = loadCache(cacheKey);
        if (cached != null && age(cached.savedAt()).compareTo(FRESH_CACHE) <= 0) {
            return markCached(cached.prices());
        }

        try {
            List<MandiPrice> all = fetchWithRetry(crop, normalizedState, normalizedDistrict);

            List<MandiPrice> exact = all.stream()
                    .filter(p -> LocationNormalizer.samePlace(p.district(), normalizedDistrict))
                    .map(p -> copy(p, true, false))
                    .toList();

            List<MandiPrice> result = exact.isEmpty()
                    ? all.stream().map(p -> copy(p, false, false)).toList()
                    : exact;

            if (result.isEmpty()) {
                throw new ApiException(
                        crop + " ke liye " + normalizedState + " me current mandi record nahi mila."
                );
            }

            CacheEnvelope envelope = new CacheEnvelope(Instant.now(), result);
            memoryCache.put(cacheKey, envelope);
            return result;
        } catch (RuntimeException e) {
            if (cached != null && age(cached.savedAt()).compareTo(STALE_CACHE) <= 0) {
                return markCached(cached.prices());
            }
            if (e instanceof ApiException api) throw api;
            throw new ApiException(
                    "Latest mandi data unavailable hai aur usable real cache bhi nahi mila.",
                    e
            );
        }
    }

    private List<MandiPrice> fetchWithRetry(String crop, String state, String district) {
        if (Instant.now().isBefore(rateLimitedUntil)) {
            throw new ApiException(
                    "data.gov.in abhi temporary cooldown me hai; alternate real AGMARKNET source try kiya jayega."
            );
        }

        ApiException last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return fetchOnce(crop, state, district);
            } catch (RateLimitException e) {
                long waitSeconds = e.retryAfterSeconds > 0
                        ? Math.min(300L, e.retryAfterSeconds)
                        : 300L;
                rateLimitedUntil = Instant.now().plusSeconds(waitSeconds);
                throw new ApiException(
                        "data.gov.in ne HTTP 429 rate-limit diya. Repeated retry karke limit aur badhane ke bajay "
                                + "app alternate real AGMARKNET source use karega.",
                        e
                );
            } catch (RetryableApiException e) {
                last = new ApiException(
                        "data.gov.in temporary unavailable hai (HTTP " + e.status + ").",
                        e
                );
                if (attempt < MAX_ATTEMPTS) sleep(1500L);
            }
        }

        throw last == null
                ? new ApiException("Mandi service request complete nahi ho payi.")
                : last;
    }

    private List<MandiPrice> fetchOnce(String crop, String state, String district) {
        try {
            throttleLiveRequests();
            String url = "https://api.data.gov.in/resource/" + RESOURCE
                    + "?api-key=" + enc(key)
                    + "&format=json"
                    + "&limit=1000"
                    + "&offset=0"
                    + "&filters%5Bcommodity%5D=" + enc(crop)
                    + "&filters%5Bstate%5D=" + enc(state);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("User-Agent", "AgriTech-Intelligence/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = http().send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200) {
                return parse(response.body(), district, false);
            }
            if (status == 401 || status == 403) {
                throw new ApiException(
                        "Mandi API key invalid/unauthorized lag rahi hai (HTTP " + status + ")."
                );
            }
            if (status == 429) {
                throw new RateLimitException(retryAfter(response));
            }
            if (status >= 500) {
                throw new RetryableApiException(status);
            }

            throw new ApiException("Mandi service HTTP " + status + " return kar rahi hai.");
        } catch (RateLimitException | RetryableApiException | ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Mandi service reach nahi ho pa rahi. Internet check kijiye.", e);
        }
    }

    public List<MandiPrice> parse(String body, String requestedDistrict, boolean cached) {
        try {
            JsonNode records = Json.MAPPER.readTree(body).path("records");
            if (!records.isArray()) {
                throw new ApiException("Mandi response malformed hai.");
            }

            List<MandiPrice> out = new ArrayList<>();
            for (JsonNode node : records) {
                String district = text(node, "district");
                double modal = num(node, "modal_price");
                if (modal <= 0) continue;

                out.add(new MandiPrice(
                        text(node, "commodity"),
                        text(node, "state"),
                        district,
                        text(node, "market"),
                        num(node, "min_price"),
                        num(node, "max_price"),
                        modal,
                        0,
                        date(text(node, "arrival_date")),
                        text(node, "variety"),
                        SOURCE,
                        LocationNormalizer.samePlace(district, requestedDistrict),
                        cached,
                        Instant.now()
                ));
            }

            out.sort(
                    Comparator.comparing(MandiPrice::priceDate, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(MandiPrice::modalPrice, Comparator.reverseOrder())
            );
            return out;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Mandi response read nahi ho paya.", e);
        }
    }

    private CacheEnvelope loadCache(String cacheKey) {
        return memoryCache.get(cacheKey);
    }


    private List<MandiPrice> markCached(List<MandiPrice> prices) {
        return prices.stream()
                .map(p -> copy(p, p.exactDistrict(), true))
                .toList();
    }

    private Duration age(Instant instant) {
        if (instant == null) return Duration.ofDays(999);
        return Duration.between(instant, Instant.now()).abs();
    }


    private void throttleLiveRequests() {
        long elapsed = Duration.between(lastLiveCall, Instant.now()).toMillis();
        long minimumGap = 1500L;
        if (elapsed < minimumGap) sleep(minimumGap - elapsed);
        lastLiveCall = Instant.now();
    }

    private long retryAfter(HttpResponse<?> response) {
        try {
            return response.headers().firstValue("Retry-After")
                    .map(Long::parseLong)
                    .orElse(0L);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private long backoffMillis(int attempt, long retryAfterSeconds) {
        if (retryAfterSeconds > 0) {
            return Math.min(10_000L, retryAfterSeconds * 1000L);
        }
        return switch (attempt) {
            case 1 -> 1200L;
            case 2 -> 2500L;
            default -> 5000L;
        };
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Mandi retry interrupted ho gaya.", e);
        }
    }

    private String cacheKey(String crop, String state, String district) {
        return (crop + "|" + state + "|" + district).toLowerCase(Locale.ROOT);
    }


    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String key) {
        return node.path(key).asText("");
    }

    private double num(JsonNode node, String key) {
        try {
            return Double.parseDouble(text(node, key));
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDate date(String value) {
        for (String pattern : List.of("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private MandiPrice copy(MandiPrice p, boolean exact, boolean cached) {
        return new MandiPrice(
                p.cropName(), p.state(), p.district(), p.mandiName(),
                p.minPrice(), p.maxPrice(), p.modalPrice(), p.changePercent(),
                p.priceDate(), p.variety(), p.source(), exact, cached,
                p.retrievedAt() == null ? Instant.now() : p.retrievedAt()
        );
    }

    public record CacheEnvelope(Instant savedAt, List<MandiPrice> prices) {}

    private static final class RateLimitException extends RuntimeException {
        private final long retryAfterSeconds;
        private RateLimitException(long retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    private static final class RetryableApiException extends RuntimeException {
        private final int status;
        private RetryableApiException(int status) {
            this.status = status;
        }
    }
}
