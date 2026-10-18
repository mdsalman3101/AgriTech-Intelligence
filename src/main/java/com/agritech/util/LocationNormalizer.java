package com.agritech.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

public final class LocationNormalizer {
    private static final Map<String, String> STATE_ALIASES = Map.ofEntries(
            Map.entry("gujrat", "Gujarat"),
            Map.entry("gujarat", "Gujarat"),
            Map.entry("rajasthan", "Rajasthan"),
            Map.entry("bihar", "Bihar"),
            Map.entry("maharashtra", "Maharashtra"),
            Map.entry("madhyapradesh", "Madhya Pradesh"),
            Map.entry("uttarpradesh", "Uttar Pradesh")
    );

    private LocationNormalizer() {}

    public static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeStateForStorage(String value) {
        String cleaned = clean(value);
        String key = key(cleaned);
        return STATE_ALIASES.getOrDefault(key, titleCase(cleaned));
    }

    public static String normalizeDistrictForStorage(String value) {
        String cleaned = clean(value);
        String key = key(cleaned);
        if (key.equals("mahesana") || key.equals("mehsana")) return "Mehsana";
        if (key.equals("ahmadabad") || key.equals("ahmedabad")) return "Ahmedabad";
        return titleCase(cleaned);
    }

    public static boolean samePlace(String a, String b) {
        String x = canonicalKey(a);
        String y = canonicalKey(b);
        return !x.isBlank() && x.equals(y);
    }

    public static String canonicalKey(String value) {
        String k = key(value);
        if (k.equals("gujrat")) return "gujarat";
        if (k.equals("mahesana")) return "mehsana";
        if (k.equals("ahmadabad")) return "ahmedabad";
        return k;
    }

    public static String ascii(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\x20-\\x7E]", "")
                .trim();
    }

    public static String key(String value) {
        String ascii = ascii(clean(value)).toLowerCase(Locale.ROOT);
        return ascii
                .replaceAll("\\b(district|city|taluka|tehsil)\\b", "")
                .replaceAll("[^a-z0-9]", "");
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "";
        String[] words = clean(value).toLowerCase(Locale.ROOT).split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }
}
