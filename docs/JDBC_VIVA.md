# JDBC Viva Quick Notes

**What is JDBC?**  
JDBC (Java Database Connectivity) is the standard Java API used to connect Java applications with relational databases such as MySQL.

**Where is JDBC used in this project?**  
Farmer profiles, equipment inventory and equipment booking history are stored in MySQL through JDBC repositories.

**Main JDBC classes used:**
- `DriverManager`
- `Connection`
- `PreparedStatement`
- `ResultSet`

**Why PreparedStatement?**  
It keeps SQL separate from user values, reduces SQL-injection risk and handles parameter types safely.

**Why try-with-resources?**  
It automatically closes database connections, statements and result sets.

**Why transaction in equipment booking?**  
Booking must reduce inventory and create the booking record together. If either step fails, rollback keeps the database consistent.

**What does `SELECT ... FOR UPDATE` do?**  
It locks the selected equipment row inside the transaction so simultaneous bookings cannot incorrectly use the same final available unit.

**Why MySQL instead of CSV files?**  
MySQL provides structured storage, constraints, unique keys, transactions, querying and safer concurrent updates.
