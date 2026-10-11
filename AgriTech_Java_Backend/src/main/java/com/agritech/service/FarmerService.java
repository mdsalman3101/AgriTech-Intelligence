package com.agritech.service;

import com.agritech.enums.SoilType;
import com.agritech.model.Farmer;
import com.agritech.repository.FarmerRepository;
import com.agritech.util.IdGenerator;

public class FarmerService {
    private final FarmerRepository repository;

    public FarmerService(FarmerRepository repository) {
        this.repository = repository;
    }

    public Farmer register(String name, String village, String district, String state,
                           double landArea, SoilType soilType, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name required.");
        if (landArea <= 0) throw new IllegalArgumentException("Land area must be > 0.");
        if (phone == null || !phone.matches("\\d{10}"))
            throw new IllegalArgumentException("Phone must have 10 digits.");

        Farmer f = new Farmer(IdGenerator.farmerId(), name, village, district, state, landArea, soilType, phone);
        repository.save(f);
        return f;
    }

    public Farmer getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Farmer not found."));
    }

    public void seed(Farmer farmer) {
        repository.save(farmer);
    }
}
