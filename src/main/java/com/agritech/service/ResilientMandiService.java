package com.agritech.service;

import com.agritech.exception.ApiException;
import com.agritech.model.MandiPrice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tries multiple genuine market-data providers in order.
 * No mock/demo mandi price is created here.
 */
public class ResilientMandiService implements MandiService {
    private final List<MandiService> providers;

    public ResilientMandiService(MandiService primary, MandiService fallback) {
        this.providers = List.of(primary, fallback);
    }

    public ResilientMandiService(MandiService... providers) {
        this.providers = Arrays.stream(providers)
                .filter(p -> p != null)
                .toList();
    }

    @Override
    public List<MandiPrice> findPrices(String crop, String state, String district) {
        List<String> errors = new ArrayList<>();

        for (MandiService provider : providers) {
            try {
                List<MandiPrice> prices = provider.findPrices(crop, state, district);
                if (prices != null && !prices.isEmpty()) return prices;
            } catch (RuntimeException e) {
                String message = e.getMessage();
                if (message != null && !message.isBlank()) errors.add(message);
            }
        }

        String detail = errors.isEmpty()
                ? "Kisi real source se market record nahi mila."
                : String.join(" | ", errors);

        throw new ApiException(
                "Genuine mandi data abhi available nahi ho paya. " + detail
        );
    }
}
