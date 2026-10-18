package com.agritech.repository;

import com.agritech.database.DatabaseManager;
import com.agritech.enums.SoilType;
import com.agritech.model.Farmer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MySQL implementation of FarmerRepository using JDBC PreparedStatements. */
public class JdbcFarmerRepository implements FarmerRepository {
    private final DatabaseManager database;

    public JdbcFarmerRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public void save(Farmer farmer) {
        String sql = """
                INSERT INTO farmers
                (farmer_id, name, village, district, state, land_area, soil_type, phone, irrigation_available)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, farmer);
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new IllegalArgumentException("Farmer ID ya phone number already registered hai.");
            }
            throw new IllegalStateException("Farmer MySQL me save nahi ho paya: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Farmer farmer) {
        String sql = """
                UPDATE farmers
                SET name=?, village=?, district=?, state=?, land_area=?, soil_type=?, phone=?,
                    irrigation_available=?, updated_at=CURRENT_TIMESTAMP
                WHERE farmer_id=?
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, farmer.getName());
            statement.setString(2, farmer.getVillage());
            statement.setString(3, farmer.getDistrict());
            statement.setString(4, farmer.getState());
            statement.setDouble(5, farmer.getLandArea());
            statement.setString(6, farmer.getSoilType().name());
            statement.setString(7, farmer.getPhone());
            statement.setBoolean(8, farmer.isIrrigationAvailable());
            statement.setString(9, farmer.getFarmerId());
            int changed = statement.executeUpdate();
            if (changed == 0) throw new IllegalArgumentException("Farmer ID nahi mila.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new IllegalArgumentException("Ye phone number doosre farmer ke saath registered hai.");
            }
            throw new IllegalStateException("Farmer MySQL me update nahi ho paya: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Farmer> findById(String farmerId) {
        String sql = "SELECT * FROM farmers WHERE farmer_id=?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, farmerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Farmer MySQL se read nahi ho paya: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Farmer> findAll() {
        String sql = "SELECT * FROM farmers ORDER BY created_at DESC, farmer_id";
        List<Farmer> farmers = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) farmers.add(map(result));
            return farmers;
        } catch (SQLException e) {
            throw new IllegalStateException("Farmer list MySQL se read nahi ho payi: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement statement, Farmer farmer) throws SQLException {
        statement.setString(1, farmer.getFarmerId());
        statement.setString(2, farmer.getName());
        statement.setString(3, farmer.getVillage());
        statement.setString(4, farmer.getDistrict());
        statement.setString(5, farmer.getState());
        statement.setDouble(6, farmer.getLandArea());
        statement.setString(7, farmer.getSoilType().name());
        statement.setString(8, farmer.getPhone());
        statement.setBoolean(9, farmer.isIrrigationAvailable());
    }

    private Farmer map(ResultSet result) throws SQLException {
        return new Farmer(
                result.getString("farmer_id"),
                result.getString("name"),
                result.getString("village"),
                result.getString("district"),
                result.getString("state"),
                result.getDouble("land_area"),
                SoilType.valueOf(result.getString("soil_type")),
                result.getString("phone"),
                result.getBoolean("irrigation_available")
        );
    }
}
