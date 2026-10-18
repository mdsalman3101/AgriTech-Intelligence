package com.agritech.config;

/**
 * Secrets/configuration are read from environment variables only.
 * No local credential file is used by the final JDBC build.
 */
public final class ConfigLoader {

    public String get(String envName, String legacyPropertyName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank() || value.startsWith("YOUR_")) return null;
        return value.trim();
    }

    public boolean configured(String envName, String legacyPropertyName) {
        return get(envName, legacyPropertyName) != null;
    }
}
