package com.agritech.service;

import com.agritech.exception.ApiException;
import com.agritech.infrastructure.HttpClientProvider;
import com.agritech.infrastructure.Json;
import com.agritech.model.MandiPrice;
import com.agritech.util.LocationNormalizer;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Public no-key fallback using CEDA's Agri-Market interface.
 * CEDA attributes its underlying market data to DMI/AGMARKNET.
 *
 * This adapter deliberately labels the output as "latest available"
 * rather than pretending that the value is an intraday/live quote.
 */
public class CedaMandiService implements MandiService {
    public static final String SOURCE = "CEDA Agri Market (DMI/AGMARKNET-derived fallback)";

    private static final String BASE = "https://agmarknet.ceda.ashoka.edu.in/api";
    private static final Duration FRESH_CACHE = Duration.ofHours(12);
    private static final Duration STALE_CACHE = Duration.ofDays(30);

    private static final Map<String, Integer> STATE_IDS = stateIds();

    private final HttpClient client;
    private final Map<String, CacheEnvelope> memoryCache = new HashMap<>();
    private final Map<String, Integer> commodityIdCache = new HashMap<>();

    public CedaMandiService() {
        this(HttpClientProvider.create());
    }

    public CedaMandiService(HttpClient client) {
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
            List<MandiPrice> result = fetchLatestAvailable(normalizedCrop, normalizedState, normalizedDistrict);
            if (result.isEmpty()) {
                throw new ApiException("CEDA me " + normalizedCrop + " ka latest available market record nahi mila.");
            }

            CacheEnvelope envelope = new CacheEnvelope(Instant.now(), result);
            memoryCache.put(key, envelope);
            return result;
        } catch (RuntimeException e) {
            if (cached != null && age(cached.savedAt()).compareTo(STALE_CACHE) <= 0) {
                return markCached(cached.prices());
            }
            if (e instanceof ApiException api) throw api;
            throw new ApiException("CEDA mandi fallback reach nahi ho pa rahi.", e);
        }
    }

    private List<MandiPrice> fetchLatestAvailable(String crop, String state, String district) {
        Integer stateId = STATE_IDS.get(LocationNormalizer.canonicalKey(state));
        if (stateId == null) {
            throw new ApiException("CEDA fallback me state mapping available nahi hai: " + state);
        }

        int commodityId = resolveCommodityId(crop);

        LocalDate today = LocalDate.now();

        // Fast path: recent daily series.
        List<MandiPrice> daily = fetchPrices(
                crop, state, district, stateId, commodityId,
                today.minusDays(90), today, "d"
        );
        if (!daily.isEmpty()) return daily;

        // Wider fallback: latest available monthly series from the last 24 months.
        return fetchPrices(
                crop, state, district, stateId, commodityId,
                today.minusYears(2), today, "m"
        );
    }

    private int resolveCommodityId(String crop) {
        String key = LocationNormalizer.canonicalKey(crop);
        Integer cached = commodityIdCache.get(key);
        if (cached != null) return cached;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + "/commodities"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Origin", "https://agmarknet.ceda.ashoka.edu.in")
                    .header("Referer", "https://agmarknet.ceda.ashoka.edu.in/")
                    .header("User-Agent", browserUserAgent())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ApiException("CEDA commodity list HTTP " + response.statusCode() + " return kar rahi hai.");
            }

            JsonNode root = Json.MAPPER.readTree(response.body());
            JsonNode rows = root;
            if (root.isObject()) {
                for (String field : List.of("data", "commodities", "results")) {
                    JsonNode candidate = root.path(field);
                    if (candidate.isArray()) {
                        rows = candidate;
                        break;
                    }
                }
            }

            if (!rows.isArray()) {
                throw new ApiException("CEDA commodity response format samajh nahi aaya.");
            }

            for (JsonNode row : rows) {
                String name = firstText(row,
                        "commodity", "commodity_name", "commodityName", "name", "label", "cmdty");
                if (!LocationNormalizer.samePlace(name, crop)) continue;

                int id = firstInt(row,
                        "commodity_id", "commodityId", "id", "value", "cmdty_id");
                if (id > 0) {
                    commodityIdCache.put(key, id);
                    return id;
                }
            }

            throw new ApiException("CEDA commodity id nahi mila: " + crop);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("CEDA commodity lookup fail ho gaya.", e);
        }
    }

    private List<MandiPrice> fetchPrices(
            String crop,
            String state,
            String district,
            int stateId,
            int commodityId,
            LocalDate start,
            LocalDate end,
            String calculationType
    ) {
        try {
            String payload = "{" +
                    "\"state_id\":" + stateId + "," +
                    "\"commodity_id\":" + commodityId + "," +
                    "\"district_id\":0," +
                    "\"calculation_type\":\"" + calculationType + "\"," +
                    "\"chart_type\":\"timeseries\"," +
                    "\"start_date\":\"" + iso(start) + "\"," +
                    "\"end_date\":\"" + iso(end) + "\"" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + "/prices"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://agmarknet.ceda.ashoka.edu.in")
                    .header("Referer", "https://agmarknet.ceda.ashoka.edu.in/")
                    .header("User-Agent", browserUserAgent())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200) {
                return parsePriceResponse(response.body(), crop, state, district, false);
            }
            if (status == 404) return List.of();
            if (status == 429) {
                throw new ApiException("CEDA bhi temporary rate-limit kar rahi hai (HTTP 429).");
            }
            throw new ApiException("CEDA mandi fallback HTTP " + status + " return kar rahi hai.");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("CEDA mandi fallback reach nahi ho pa rahi.", e);
        }
    }

    /** Public parser kept deterministic for tests. */
    public List<MandiPrice> parsePriceResponse(
            String body,
            String crop,
            String state,
            String requestedDistrict,
            boolean cached
    ) {
        try {
            JsonNode root = Json.MAPPER.readTree(body);
            JsonNode rows = root.isArray() ? root : root.path("data");
            if (!rows.isArray()) {
                throw new ApiException("CEDA mandi response format samajh nahi aaya.");
            }

            List<MandiPrice> out = new ArrayList<>();
            for (JsonNode row : rows) {
                double modal = firstNumber(row, "p_modal", "modal_price", "modalPrice", "modal");
                if (modal <= 0) continue;

                LocalDate priceDate = parseDate(firstText(row, "t", "date", "arrival_date", "arrivalDate"));
                String commodity = firstText(row, "cmdty", "commodity", "commodity_name", "commodityName");
                String displayCrop = commodity.isBlank() ? crop : commodity;

                out.add(new MandiPrice(
                        displayCrop,
                        LocationNormalizer.normalizeStateForStorage(state),
                        LocationNormalizer.normalizeStateForStorage(state) + " (state-wide)",
                        "CEDA state aggregate",
                        firstNumber(row, "p_min", "min_price", "minPrice"),
                        firstNumber(row, "p_max", "max_price", "maxPrice"),
                        modal,
                        0,
                        priceDate,
                        "",
                        SOURCE,
                        false,
                        cached,
                        Instant.now()
                ));
            }

            out.sort(
                    Comparator.comparing(MandiPrice::priceDate,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(MandiPrice::modalPrice, Comparator.reverseOrder())
            );
            return out;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("CEDA mandi response read nahi ho paya.", e);
        }
    }

    private String iso(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC).toString();
    }

    private String browserUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36";
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("");
            if (!value.isBlank()) return LocationNormalizer.ascii(value);
        }
        return "";
    }

    private int firstInt(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isNumber()) return value.asInt();
            String text = value.asText("");
            if (!text.isBlank()) {
                try {
                    return Integer.parseInt(text.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
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
        String clean = value.length() >= 10 ? value.substring(0, 10) : value;
        for (String pattern : List.of("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "MM/yyyy")) {
            try {
                if (pattern.equals("MM/yyyy")) {
                    return java.time.YearMonth.parse(clean, DateTimeFormatter.ofPattern(pattern)).atDay(1);
                }
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern(pattern));
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
        Map<String, Integer> raw = new HashMap<>();
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
