package com.agritech.repository;

import com.agritech.database.DatabaseManager;
import com.agritech.exception.EquipmentNotAvailableException;
import com.agritech.model.equipment.FarmEquipment;
import com.agritech.model.equipment.Harvester;
import com.agritech.model.equipment.IrrigationPump;
import com.agritech.model.equipment.Rotavator;
import com.agritech.model.equipment.Tractor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC equipment inventory with transactional booking/return. */
public class JdbcEquipmentRepository implements EquipmentRepository {
    private final DatabaseManager database;

    public JdbcEquipmentRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public List<FarmEquipment> findAll() {
        String sql = "SELECT equipment_id, equipment_type, daily_rate, available_units FROM equipment ORDER BY equipment_type, equipment_id";
        List<FarmEquipment> items = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                items.add(make(
                        result.getString("equipment_id"),
                        result.getString("equipment_type"),
                        result.getDouble("daily_rate"),
                        result.getInt("available_units")
                ));
            }
            return items;
        } catch (SQLException e) {
            throw new IllegalStateException("Equipment inventory MySQL se read nahi ho payi: " + e.getMessage(), e);
        }
    }

    @Override
    public void add(String id, String type, double dailyRate, int quantity) {
        FarmEquipment equipment = make(id, type, dailyRate, quantity);
        String sql = """
                INSERT INTO equipment (equipment_id, equipment_type, daily_rate, available_units)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, equipment.getId());
            statement.setString(2, canonicalType(type));
            statement.setDouble(3, equipment.getDailyRate());
            statement.setInt(4, equipment.getAvailableUnits());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new IllegalArgumentException("Equipment ID already exists.");
            throw new IllegalStateException("Equipment MySQL me add nahi ho paya: " + e.getMessage(), e);
        }
    }

    @Override
    public double book(String id, int days) {
        if (days <= 0) throw new IllegalArgumentException("Rental days 0 se greater hone chahiye.");

        String select = "SELECT equipment_type, daily_rate, available_units FROM equipment WHERE equipment_id=? FOR UPDATE";
        String update = "UPDATE equipment SET available_units=available_units-1, updated_at=CURRENT_TIMESTAMP WHERE equipment_id=?";
        String insertBooking = """
                INSERT INTO equipment_bookings (equipment_id, rental_days, total_amount, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """;

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                double dailyRate;
                int available;
                String type;
                try (PreparedStatement statement = connection.prepareStatement(select)) {
                    statement.setString(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Equipment ID nahi mila.");
                        type = result.getString("equipment_type");
                        dailyRate = result.getDouble("daily_rate");
                        available = result.getInt("available_units");
                    }
                }

                if (available <= 0) {
                    throw new EquipmentNotAvailableException(type + " abhi available nahi hai.");
                }

                double total = dailyRate * days;
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    statement.setString(1, id);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(insertBooking)) {
                    statement.setString(1, id);
                    statement.setInt(2, days);
                    statement.setDouble(3, total);
                    statement.executeUpdate();
                }

                connection.commit();
                return total;
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Equipment booking MySQL me save nahi ho payi: " + e.getMessage(), e);
        }
    }

    @Override
    public void returnOne(String id) {
        String selectEquipment = "SELECT equipment_id FROM equipment WHERE equipment_id=? FOR UPDATE";
        String findBooking = """
                SELECT booking_id FROM equipment_bookings
                WHERE equipment_id=? AND status='ACTIVE'
                ORDER BY booked_at DESC, booking_id DESC LIMIT 1 FOR UPDATE
                """;
        String closeBooking = "UPDATE equipment_bookings SET status='RETURNED', returned_at=CURRENT_TIMESTAMP WHERE booking_id=?";
        String updateEquipment = "UPDATE equipment SET available_units=available_units+1, updated_at=CURRENT_TIMESTAMP WHERE equipment_id=?";

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(selectEquipment)) {
                    statement.setString(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Equipment ID nahi mila.");
                    }
                }

                long bookingId;
                try (PreparedStatement statement = connection.prepareStatement(findBooking)) {
                    statement.setString(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Is equipment ki koi ACTIVE booking nahi mili.");
                        }
                        bookingId = result.getLong("booking_id");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(closeBooking)) {
                    statement.setLong(1, bookingId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(updateEquipment)) {
                    statement.setString(1, id);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Equipment return MySQL me save nahi ho paya: " + e.getMessage(), e);
        }
    }

    private FarmEquipment make(String id, String type, double rate, int quantity) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Equipment ID required hai.");
        if (rate < 0 || quantity < 0) throw new IllegalArgumentException("Rate/quantity negative nahi ho sakte.");
        return switch (canonicalType(type)) {
            case "Tractor" -> new Tractor(id.trim(), rate, quantity);
            case "Harvester" -> new Harvester(id.trim(), rate, quantity);
            case "Irrigation Pump" -> new IrrigationPump(id.trim(), rate, quantity);
            case "Rotavator" -> new Rotavator(id.trim(), rate, quantity);
            default -> throw new IllegalArgumentException("Unsupported equipment type.");
        };
    }

    private String canonicalType(String type) {
        if (type == null) throw new IllegalArgumentException("Equipment type required hai.");
        String normalized = type.trim().toLowerCase();
        return switch (normalized) {
            case "tractor", "tractor 50 hp" -> "Tractor";
            case "harvester" -> "Harvester";
            case "irrigation pump", "pump" -> "Irrigation Pump";
            case "rotavator" -> "Rotavator";
            default -> throw new IllegalArgumentException("Use Tractor, Harvester, Irrigation Pump or Rotavator.");
        };
    }
}
