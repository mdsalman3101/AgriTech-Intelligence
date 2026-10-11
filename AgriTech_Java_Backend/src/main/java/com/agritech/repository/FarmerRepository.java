package com.agritech.repository;

import com.agritech.model.Farmer;
import java.util.List;
import java.util.Optional;

public interface FarmerRepository {
    void save(Farmer farmer);
    Optional<Farmer> findById(String farmerId);
    List<Farmer> findAll();
}
