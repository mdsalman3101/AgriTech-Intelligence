package com.agritech.model.equipment;

public interface FarmEquipment {
    String getId();
    String getName();
    double getDailyRate();
    int getAvailableUnits();
    boolean isAvailable();
    double getRentalCost(int days);
    void book(int days);
}
