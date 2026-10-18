package com.agritech.service;

import com.agritech.enums.SoilType;
import com.agritech.model.Farmer;
import com.agritech.repository.FarmerRepository;
import com.agritech.util.IdGenerator;
import com.agritech.util.LocationNormalizer;

import java.util.List;

public class FarmerService {
    private final FarmerRepository repository;

    public FarmerService(FarmerRepository repository) {
        this.repository = repository;
    }

    public Farmer register(String name, String village, String district, String state,
                           double landArea, SoilType soilType, String phone) {
        return register(name, village, district, state, landArea, soilType, phone, false);
    }

    public Farmer register(String name, String village, String district, String state,
                           double landArea, SoilType soilType, String phone, boolean irrigation) {
        validate(name, village, district, state, landArea, soilType, phone, null);

        String normalizedPhone = phone.trim();
        if (repository.findAll().stream().anyMatch(x -> x.getPhone().equals(normalizedPhone))) {
            throw new IllegalArgumentException("Is phone number se farmer profile already registered hai.");
        }

        Farmer farmer = new Farmer(
                IdGenerator.farmerId(),
                clean(name),
                LocationNormalizer.clean(village),
                LocationNormalizer.normalizeDistrictForStorage(district),
                LocationNormalizer.normalizeStateForStorage(state),
                landArea,
                soilType,
                normalizedPhone,
                irrigation
        );
        repository.save(farmer);
        return farmer;
    }

    public Farmer update(String farmerId, String name, String village, String district, String state,
                         double landArea, SoilType soilType, String phone, boolean irrigation) {
        Farmer farmer = getById(farmerId);
        validate(name, village, district, state, landArea, soilType, phone, farmerId);

        String normalizedPhone = phone.trim();
        boolean duplicate = repository.findAll().stream()
                .anyMatch(x -> !x.getFarmerId().equalsIgnoreCase(farmerId)
                        && x.getPhone().equals(normalizedPhone));
        if (duplicate) {
            throw new IllegalArgumentException("Is phone number se doosra farmer profile already registered hai.");
        }

        farmer.updateProfile(
                clean(name),
                LocationNormalizer.clean(village),
                LocationNormalizer.normalizeDistrictForStorage(district),
                LocationNormalizer.normalizeStateForStorage(state),
                landArea,
                soilType,
                normalizedPhone,
                irrigation
        );
        repository.update(farmer);
        return farmer;
    }

    public Farmer getById(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Farmer ID required hai.");
        return repository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("Farmer ID nahi mila."));
    }

    public List<Farmer> getAll() {
        return repository.findAll();
    }

    private void validate(String name, String village, String district, String state,
                          double landArea, SoilType soilType, String phone, String currentId) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Farmer name required hai.");
        if (village == null || village.isBlank()
                || district == null || district.isBlank()
                || state == null || state.isBlank()) {
            throw new IllegalArgumentException("Village, district aur state required hain.");
        }
        if (landArea <= 0 || landArea > 100000) {
            throw new IllegalArgumentException("Land area 0 se greater hona chahiye.");
        }
        if (soilType == null) throw new IllegalArgumentException("Soil type required hai.");
        if (phone == null || !phone.trim().matches("[6-9]\\d{9}")) {
            throw new IllegalArgumentException("Valid 10-digit Indian mobile number enter kijiye.");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
