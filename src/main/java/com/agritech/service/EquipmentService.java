package com.agritech.service;

import com.agritech.model.equipment.FarmEquipment;
import com.agritech.repository.EquipmentRepository;

import java.util.List;

public class EquipmentService {
    private final EquipmentRepository repository;

    public EquipmentService(EquipmentRepository repository) {
        this.repository = repository;
    }

    public List<FarmEquipment> getAll() {
        return repository.findAll();
    }

    public void add(String id, String type, double rate, int quantity) {
        if (id == null || id.isBlank() || rate < 0 || quantity < 0) {
            throw new IllegalArgumentException("Valid ID, rate aur quantity enter kijiye.");
        }
        repository.add(id.trim(), type, rate, quantity);
    }

    public double book(String id, int days) {
        return repository.book(id == null ? "" : id.trim(), days);
    }

    public void returnOne(String id) {
        repository.returnOne(id == null ? "" : id.trim());
    }
}
