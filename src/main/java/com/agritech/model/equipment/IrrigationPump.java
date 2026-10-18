package com.agritech.model.equipment;

public class IrrigationPump extends BaseEquipment {
    public IrrigationPump(String id, double dailyRate, int units) {
        super(id, "Irrigation Pump", dailyRate, units);
    }
}
