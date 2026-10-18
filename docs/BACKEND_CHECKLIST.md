# Final Backend Checklist — MySQL/JDBC

- [x] Farmer registration/profile stored in MySQL
- [x] Farmer profile update stored in MySQL
- [x] JDBC `PreparedStatement` queries
- [x] Equipment inventory stored in MySQL
- [x] Equipment booking history stored in MySQL
- [x] Transactional equipment booking/return
- [x] No CSV/TXT application persistence
- [x] DB/API secrets excluded from source code
- [x] Location spelling normalization
- [x] Strict wrong-state geocoding rejection
- [x] Real Open-Meteo geocoding/weather
- [x] Explainable sowing decision engine
- [x] Mandi failover chain without fake prices
- [x] Profit estimation only when valid market evidence exists
- [x] EMI + loan affordability planning
- [x] Smart crop advisor
- [x] Hinglish local narrator
- [x] Optional Gemini narrator with local fallback
- [x] Colored terminal UI
- [x] MySQL/JDBC status visible in option 8
- [x] Workbench schema + verification SQL included
- [x] One-command run script

Before presentation:

1. Start MySQL Server.
2. Execute `sql/01_agritech_schema.sql` once.
3. Run `setup.ps1` once.
4. Start with `run.ps1`.
5. Option 8 should show `Database: CONNECTED | MySQL via JDBC`.
6. Register a farmer, then use `sql/02_verify_database.sql` in Workbench to show the inserted row.
