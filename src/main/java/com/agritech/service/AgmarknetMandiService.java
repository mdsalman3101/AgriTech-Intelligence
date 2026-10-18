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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Secondary real-data provider backed by the public AGMARKNET 2.0 API.
 *
 * This is intentionally a fallback. data.gov.in remains the primary provider,
 * but a temporary HTTP 429 from data.gov.in should not make the entire market
 * module unusable when the official AGMARKNET reporting backend is reachable.
 */
public class AgmarknetMandiService implements MandiService {
    public static final String SOURCE = "AGMARKNET 2.0 (official fallback)";

    private static final String BASE = "https://api.agmarknet.gov.in/v1";
    private static final Duration FRESH_CACHE = Duration.ofHours(6);
    private static final Duration STALE_CACHE = Duration.ofDays(7);

    // Commodity ids verified against the AGMARKNET 2.0 reporting API.
    private static final Map<String, Integer> COMMODITY_IDS = Map.of(
            "wheat", 1,
            "rice", 3,
            "maize", 4,
            "mustard", 12
    );

    // AGMARKNET uses the standard numeric state/UT codes for report queries.
    private static final Map<String, Integer> STATE_IDS = stateIds();

    private final HttpClient client;
    private final Map<String, CacheEnvelope> memoryCache = new HashMap<>();

    public AgmarknetMandiService() {
        this(HttpClientProvider.create());
    }

    public AgmarknetMandiService(HttpClient client) {
        this.client = client;
    }

    @Override
    public synchronized List<MandiPrice> findPrices(String crop, String state, String district) {
        String normalizedCrop = crop == null ? "" : crop.trim();
        String normalizedState = LocationNormalizer.normalizeStateForStorage(state);
        String normalizedDistrict = LocationNormalizer.normalizeDistrictForStorage(district);
        String key = cacheKey(normalizedCrop, normalizedState, normalizedDistrict);

        CacheEnvelope cached = loadCache(key);
        if (cached != null && age(cached.savedAt()).compareTo(FRESH_CACHE) <= 0) {
            return markCached(cached.prices());
        }

        try {
            List<MandiPrice> live = fetchLatestAvailable(normalizedCrop, normalizedState, normalizedDistrict);
            if (live.isEmpty()) {
                throw new ApiException(
                        "AGMARKNET me " + normalizedCrop + " ke liye recent market record nahi mila."
                );
            }

            CacheEnvelope envelope = new CacheEnvelope(Instant.now(), live);
            memoryCache.put(key, envelope);
            return live;
        } catch (RuntimeException e) {
            if (cached != null && age(cached.savedAt()).compareTo(STALE_CACHE) <= 0) {
                return markCached(cached.prices());
            }
            if (e instanceof ApiException api) throw api;
            throw new ApiException("AGMARKNET fallback service reach nahi ho pa rahi.", e);
        }
    }

    private List<MandiPrice> fetchLatestAvailable(String crop, String state, String district) {
        Integer commodityId = COMMODITY_IDS.get(crop.toLowerCase(Locale.ROOT));
        if (commodityId == null) {
            throw new ApiException("AGMARKNET fallback me crop mapping available nahi hai: " + crop);
        }

        Integer stateId = STATE_IDS.get(LocationNormalizer.canonicalKey(state));
        if (stateId == null) {
            throw new ApiException("AGMARKNET fallback me state mapping available nahi hai: " + state);
        }

        YearMonth month = YearMonth.now();
        for (int i = 0; i < 2; i++) {
            YearMonth target = month.minusMonths(i);
            String body = fetchMonth(target, stateId, commodityId);
            List<MandiPrice> parsed = parseMonthResponse(body, crop, state, district, false);
            if (!parsed.isEmpty()) return parsed;
        }

        return List.of();
    }

    private String fetchMonth(YearMonth month, int stateId, int commodityId) {
        try {
            String url = BASE
                    + "/prices-and-arrivals/date-wise/specific-commodity"
                    + "?year=" + month.getYear()
                    + "&month=" + month.getMonthValue()
                    + "&stateId=" + stateId
                    + "&commodityId=" + commodityId
                    + "&includeExcel=false";

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Origin", "https://agmarknet.gov.in")
                    .header("Referer", "https://agmarknet.gov.in/")
                    .header("User-Agent", "Mozilla/5.0 AgriTech-Intelligence/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200) return response.body();
            if (status == 404) return "{\"markets\":[]}";
            if (status == 429) {
                throw new ApiException("AGMARKNET fallback bhi temporarily rate-limit ho rahi hai (HTTP 429).");
            }
            if (status == 403) {
                throw new ApiException(
                        "AGMARKNET fallback request block hui (HTTP 403). India network se dobara try kijiye."
                );
            }
            throw new ApiException("AGMARKNET fallback HTTP " + status + " return kar rahi hai.");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("AGMARKNET fallback service reach nahi ho pa rahi.", e);
        }
    }

    /**
     * Public deterministic parser used by tests. The upstream response is
     * market -> date -> variety rows. Results are ordered so an apparent
     * district-name match is preferred, then newest data, then modal price.
     */
    public List<MandiPrice> parseMonthResponse(
            String body,
            String crop,
            String state,
            String requestedDistrict,
            boolean cached
    ) {
        try {
            JsonNode root = Json.MAPPER.readTree(body);
            JsonNode markets = root.path("markets");
            if (!markets.isArray()) {
                JsonNode data = root.path("data");
                if (data.isObject()) markets = data.path("markets");
            }
            if (!markets.isArray()) {
                throw new ApiException("AGMARKNET response format samajh nahi aaya.");
            }

            List<MandiPrice> out = new ArrayList<>();
            for (JsonNode market : markets) {
                String marketName = firstText(market, "marketName", "market_name", "market");
                JsonNode dates = market.path("dates");
                if (!dates.isArray()) continue;

                for (JsonNode dateNode : dates) {
                    LocalDate priceDate = parseDate(firstText(dateNode, "arrivalDate", "arrival_date", "date"));
                    JsonNode rows = dateNode.path("data");
                    if (!rows.isArray()) continue;

                    for (JsonNode row : rows) {
                        double modal = firstNumber(row, "modalPrice", "modal_price", "modal");
                        if (modal <= 0) continue;

                        boolean districtMatch = marketLooksLikeDistrict(marketName, requestedDistrict);
                        String displayDistrict = districtMatch
                                ? LocationNormalizer.normalizeDistrictForStorage(requestedDistrict)
                                : LocationNormalizer.normalizeStateForStorage(state) + " (state-wide)";

                        out.add(new MandiPrice(
                                crop,
                                LocationNormalizer.normalizeStateForStorage(state),
                                displayDistrict,
                                marketName.isBlank() ? "AGMARKNET market" : marketName,
                                firstNumber(row, "minimumPrice", "min_price", "minPrice"),
                                firstNumber(row, "maximumPrice", "max_price", "maxPrice"),
                                modal,
                                0,
                                priceDate,
                                firstText(row, "variety", "varietyName"),
                                SOURCE,
                                districtMatch,
                                cached,
                                Instant.now()
                        ));
                    }
                }
            }

            out.sort(
                    Comparator.comparing(MandiPrice::exactDistrict).reversed()
                            .thenComparing(MandiPrice::priceDate, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(MandiPrice::modalPrice, Comparator.reverseOrder())
            );
            return out;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("AGMARKNET response read nahi ho paya.", e);
        }
    }

    private boolean marketLooksLikeDistrict(String marketName, String district) {
        if (marketName == null || marketName.isBlank() || district == null || district.isBlank()) return false;
        String marketKey = LocationNormalizer.canonicalKey(marketName);
        String districtKey = LocationNormalizer.canonicalKey(district);
        return !districtKey.isBlank() && marketKey.contains(districtKey);
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("");
            if (!value.isBlank()) return LocationNormalizer.ascii(value);
        }
        return "";
    }

    private double firstNumber(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isNumber()) return value.asDouble();
            String text = value.asText("");
            if (!text.isBlank()) {
                try {
                    return Double.parseDouble(text.replace(",", ""));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (String pattern : List.of("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private CacheEnvelope loadCache(String key) {
        return memoryCache.get(key);
    }


    private List<MandiPrice> markCached(List<MandiPrice> prices) {
        return prices.stream()
                .map(p -> new MandiPrice(
                        p.cropName(), p.state(), p.district(), p.mandiName(),
                        p.minPrice(), p.maxPrice(), p.modalPrice(), p.changePercent(),
                        p.priceDate(), p.variety(), p.source(), p.exactDistrict(), true,
                        p.retrievedAt()
                ))
                .toList();
    }

    private Duration age(Instant savedAt) {
        if (savedAt == null) return Duration.ofDays(999);
        return Duration.between(savedAt, Instant.now()).abs();
    }

    private String cacheKey(String crop, String state, String district) {
        return (crop + "|" + state + "|" + district).toLowerCase(Locale.ROOT);
    }


    private static Map<String, Integer> stateIds() {
        Map<String, Integer> raw = new LinkedHashMap<>();
        raw.put("jammuandkashmir", 1);
        raw.put("himachalpradesh", 2);
        raw.put("punjab", 3);
        raw.put("chandigarh", 4);
        raw.put("uttarakhand", 5);
        raw.put("haryana", 6);
        raw.put("delhi", 7);
        raw.put("rajasthan", 8);
        raw.put("uttarpradesh", 9);
        raw.put("bihar", 10);
        raw.put("sikkim", 11);
        raw.put("arunachalpradesh", 12);
        raw.put("nagaland", 13);
        raw.put("manipur", 14);
        raw.put("mizoram", 15);
        raw.put("tripura", 16);
        raw.put("meghalaya", 17);
        raw.put("assam", 18);
        raw.put("westbengal", 19);
        raw.put("jharkhand", 20);
        raw.put("odisha", 21);
        raw.put("chhattisgarh", 22);
        raw.put("madhyapradesh", 23);
        raw.put("gujarat", 24);
        raw.put("damananddiu", 25);
        raw.put("dadraandnagarhaveli", 26);
        raw.put("maharashtra", 27);
        raw.put("andhrapradesh", 28);
        raw.put("karnataka", 29);
        raw.put("goa", 30);
        raw.put("lakshadweep", 31);
        raw.put("kerala", 32);
        raw.put("tamilnadu", 33);
        raw.put("puducherry", 34);
        raw.put("andamanandnicobarislands", 35);
        raw.put("telangana", 36);
        raw.put("ladakh", 37);
        return Map.copyOf(raw);
    }

    public record CacheEnvelope(Instant savedAt, List<MandiPrice> prices) {}
}
