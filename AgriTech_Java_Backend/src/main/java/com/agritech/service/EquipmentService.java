package com.agritech.service;

import com.agritech.model.equipment.FarmEquipment;
import java.util.List;

public class EquipmentService {
    private final List<FarmEquipment> equipment;

    public EquipmentService(List<FarmEquipment> equipment) {
        this.equipment = equipment;
    }

    public List<FarmEquipment> getAll() { return List.copyOf(equipment); }

    public double book(String id, int days) {
        FarmEquipment e = equipment.stream()
                .filter(x -> x.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));
        double total = e.getRentalCost(days);
        e.book(days);
        return total;
    }
}
