# Package Guide

- `com.agritech.app` - terminal application flow
- `com.agritech.config` - environment-based API configuration
- `com.agritech.database` - central MySQL/JDBC connection manager
- `com.agritech.engine` - deterministic sowing decision engine
- `com.agritech.enums` - soil, water and risk enums
- `com.agritech.exception` - API/equipment domain exceptions
- `com.agritech.infrastructure` - Java HttpClient and Jackson helpers
- `com.agritech.model` - farmer, weather, market, profit, loan and advisory models
- `com.agritech.model.crop` - centralized crop reference classes
- `com.agritech.model.equipment` - OOP equipment hierarchy
- `com.agritech.narrator` - local/Gemini Hinglish advisory formatting
- `com.agritech.repository` - JDBC repository abstractions/implementations
- `com.agritech.service` - weather, geocoding, mandi, profit, loan, equipment and smart-advisor services
- `com.agritech.util` - input, colored console and location normalization helpers

Application persistence is MySQL/JDBC. CSV/TXT farmer/equipment persistence is not used in this build.
