# AgriTech Intelligence — MySQL/JDBC Final

AgriTech Intelligence is a Java 17+ terminal-based **Smart Farming Decision Assistant**. This final build keeps the existing real weather, crop, sowing, mandi, profit, equipment and loan features, but replaces local CSV/TXT persistence with **MySQL + JDBC**.

## What changed in this final build

- Farmer profiles are stored in MySQL, not files.
- Equipment inventory is stored in MySQL, not files.
- Equipment bookings are stored transactionally in MySQL.
- JDBC uses `PreparedStatement` for SQL parameters.
- Database/API secrets are read from Windows user environment variables, not source files.
- Runtime CSV/TXT persistence was removed.
- Mandi fallback caches are in-memory only; fake/demo prices are never inserted.
- Existing Open-Meteo, Mandi failover, Hinglish narration, colored terminal, crop advisor, sowing, profit and loan logic remain available.

## Architecture

```text
Terminal UI
   |
   +--> FarmerService --> FarmerRepository --> JdbcFarmerRepository --> MySQL
   |
   +--> EquipmentService --> JdbcEquipmentRepository --> MySQL
   |
   +--> WeatherService --> Open-Meteo
   |
   +--> MandiService --> AGMARKNET / CEDA / data.gov.in
   |
   +--> SmartFarmAdvisorService / SowingDecisionEngine / ProfitService / LoanService
   |
   +--> Local Hinglish Narrator or optional Gemini narrator
```

## MySQL tables

The Workbench script creates:

- `farmers`
- `equipment`
- `equipment_bookings`

No fake/demo farmer or equipment records are seeded.

## First-time setup

### 1. Start MySQL

Open MySQL Workbench and make sure your local MySQL Server is running.

### 2. Run the schema

Open:

```text
sql/01_agritech_schema.sql
```

Execute the complete script using the lightning button.

It creates database:

```text
agritech_intelligence
```

### 3. Configure JDBC + API secrets

From the project folder run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

It asks for:

- MySQL host (default `localhost`)
- MySQL port (default `3306`)
- Database name (default `agritech_intelligence`)
- MySQL username
- MySQL password
- optional `DATA_GOV_API_KEY`
- optional `GEMINI_API_KEY`

Secrets are saved as **Windows user environment variables**, not inside the project.

Open-Meteo does not require an API key for this project setup.

## Normal run

PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Or double-click:

```text
RUN_ME.bat
```

## Verify records in MySQL Workbench

After using the terminal app, run:

```text
sql/02_verify_database.sql
```

You can directly see farmer, equipment and booking records created through Java JDBC.

## Terminal features

1. **Get Complete Smart Farm Advice**
   - farmer profile + soil
   - real weather
   - water suitability
   - sowing safety
   - mandi evidence when available
   - profit estimate when real market evidence exists
   - explainable Top-3 recommendation
   - Hinglish explanation

2. **Check My Selected Crop**
   - GOOD CHOICE / POSSIBLE WITH CAUTION / NOT RECOMMENDED

3. **Weather & Best Sowing Time**
   - Open-Meteo 7-day forecast
   - sowing score, risk, window, reason and action

4. **Mandi Price & Profit Estimate**
   - AGMARKNET 2.0 -> CEDA -> data.gov.in failover
   - no fake market price
   - optional verified manual price only when user explicitly chooses it

5. **Equipment Rental**
   - MySQL inventory
   - transactional booking
   - persistent availability
   - return closes an active booking

6. **Loan / EMI Planner**
   - reducing-balance EMI
   - affordability planning estimate
   - never presented as bank approval

7. **Farmer Profile**
   - register, view, update
   - MySQL persistence through JDBC

8. **Live Data / API Status**
   - includes MySQL/JDBC connection status

## Data classification

### Live external data
- Open-Meteo weather
- Open-Meteo geocoding
- Mandi providers when reachable

### MySQL application data
- farmer profiles
- equipment inventory
- equipment booking history

### Reference data
- crop soil/temperature/water assumptions in crop classes

### Java-calculated data
- crop score
- sowing score/risk/window
- profit estimate
- EMI
- loan affordability estimate

### Optional AI text
Gemini only rewrites Java-calculated facts into friendly Hinglish. It does not decide weather, price, crop, score, profit, risk or dates.

## JDBC security points for viva

- `DriverManager` creates the MySQL connection.
- SQL values are passed with `PreparedStatement`, not string-concatenated user input.
- Connections/statements/result sets use try-with-resources.
- MySQL password and API keys are not hardcoded.
- DB operations use constraints, unique keys and transactions.
- Equipment booking uses a transaction + `SELECT ... FOR UPDATE` so stock cannot be reduced unsafely by two simultaneous operations.

## Important limitations

- Published mandi data may be delayed or providers may temporarily rate-limit requests.
- Profit is an estimate, not guaranteed income.
- Loan affordability is a planning estimate, not bank eligibility.
- The crop catalog currently contains Wheat, Rice, Maize and Mustard only.
- Real farm decisions should also consider soil testing, pests, disease, irrigation quality and local agricultural guidance.

## Testing

Normal startup intentionally does not depend on JUnit discovery:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Unit tests can be run separately:

```powershell
powershell -ExecutionPolicy Bypass -File .\test.ps1
```
