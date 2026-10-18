USE agritech_intelligence;

SHOW TABLES;
SELECT COUNT(*) AS total_farmers FROM farmers;
SELECT COUNT(*) AS equipment_items FROM equipment;
SELECT COUNT(*) AS equipment_bookings FROM equipment_bookings;

SELECT * FROM farmers ORDER BY created_at DESC;
SELECT * FROM equipment ORDER BY equipment_type;
SELECT * FROM equipment_bookings ORDER BY booking_id DESC;
