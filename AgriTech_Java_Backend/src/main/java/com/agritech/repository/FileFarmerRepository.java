package com.agritech.repository;

import com.agritech.enums.SoilType;
import com.agritech.model.Farmer;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class FileFarmerRepository implements FarmerRepository {
    private final Path file;

    public FileFarmerRepository(String filePath) {
        this.file = Paths.get(filePath);
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.createFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize farmer file.", e);
        }
    }

    public void save(Farmer farmer) {
        try {
            boolean exists = findAll().stream()
                    .anyMatch(f -> f.getFarmerId().equalsIgnoreCase(farmer.getFarmerId()));
            if (exists) return;

            String row = String.join(",",
                    farmer.getFarmerId(),
                    clean(farmer.getName()),
                    clean(farmer.getVillage()),
                    clean(farmer.getDistrict()),
                    clean(farmer.getState()),
                    String.valueOf(farmer.getLandArea()),
                    farmer.getSoilType().name(),
                    clean(farmer.getPhone())
            );
            Files.writeString(file, row + System.lineSeparator(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Could not save farmer.", e);
        }
    }

    public Optional<Farmer> findById(String farmerId) {
        return findAll().stream()
                .filter(f -> f.getFarmerId().equalsIgnoreCase(farmerId))
                .findFirst();
    }

    public List<Farmer> findAll() {
        List<Farmer> list = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) continue;
                String[] p = line.split(",", -1);
                if (p.length < 8) continue;
                list.add(new Farmer(
                        p[0], p[1], p[2], p[3], p[4],
                        Double.parseDouble(p[5]),
                        SoilType.valueOf(p[6]),
                        p[7]
                ));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read farmer file.", e);
        }
        return list;
    }

    private String clean(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
