-- =============================================================
-- AgriTech Intelligence - MySQL Workbench Schema
-- Run this complete file ONCE in MySQL Workbench.
-- No demo/fake farmer or equipment rows are inserted.
-- =============================================================

CREATE DATABASE IF NOT EXISTS agritech_intelligence
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE agritech_intelligence;

CREATE TABLE IF NOT EXISTS farmers (
    farmer_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    village VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    land_area DECIMAL(10,2) NOT NULL,
    soil_type VARCHAR(30) NOT NULL,
    phone VARCHAR(10) NOT NULL UNIQUE,
    irrigation_available BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_farmer_land CHECK (land_area > 0),
    KEY idx_farmer_location (state, district)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipment (
    equipment_id VARCHAR(30) PRIMARY KEY,
    equipment_type VARCHAR(50) NOT NULL,
    daily_rate DECIMAL(12,2) NOT NULL,
    available_units INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_equipment_rate CHECK (daily_rate >= 0),
    CONSTRAINT chk_equipment_units CHECK (available_units >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipment_bookings (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id VARCHAR(30) NOT NULL,
    rental_days INT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    status ENUM('ACTIVE','RETURNED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    returned_at TIMESTAMP NULL,
    CONSTRAINT fk_booking_equipment
        FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_rental_days CHECK (rental_days > 0),
    CONSTRAINT chk_booking_total CHECK (total_amount >= 0),
    KEY idx_booking_equipment_status (equipment_id, status, booked_at)
) ENGINE=InnoDB;

SHOW TABLES;
SELECT 'AgriTech Intelligence MySQL schema ready' AS status;
