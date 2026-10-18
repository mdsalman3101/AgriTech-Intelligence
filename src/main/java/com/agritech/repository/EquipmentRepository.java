package com.agritech.repository;

import com.agritech.model.equipment.FarmEquipment;

import java.util.List;

public interface EquipmentRepository {
    List<FarmEquipment> findAll();
    void add(String id, String type, double dailyRate, int quantity);
    double book(String id, int days);
    void returnOne(String id);
}
