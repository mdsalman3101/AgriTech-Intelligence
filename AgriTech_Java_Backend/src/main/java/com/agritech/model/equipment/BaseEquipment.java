package com.agritech.model.equipment;

import com.agritech.exception.EquipmentNotAvailableException;

public abstract class BaseEquipment implements FarmEquipment {
    private final String id;
    private final String name;
    private final double dailyRate;
    private int availableUnits;

    protected BaseEquipment(String id, String name, double dailyRate, int availableUnits) {
        this.id = id;
        this.name = name;
        this.dailyRate = dailyRate;
        this.availableUnits = availableUnits;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getDailyRate() { return dailyRate; }
    public int getAvailableUnits() { return availableUnits; }

    public boolean isAvailable() { return availableUnits > 0; }

    public double getRentalCost(int days) {
        if (days <= 0) throw new IllegalArgumentException("Days must be greater than 0.");
        return dailyRate * days;
    }

    public void book(int days) {
        getRentalCost(days);
        if (!isAvailable()) throw new EquipmentNotAvailableException(name + " not available.");
        availableUnits--;
    }
}
