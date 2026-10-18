# Data Sources

| Data | Type | Source / Storage | Fallback |
|---|---|---|---|
| Farmer profile | User input | MySQL via JDBC | None |
| Equipment inventory | User/admin input | MySQL via JDBC | None |
| Equipment bookings | Application transaction | MySQL via JDBC | None |
| Place resolution | Live | Open-Meteo Geocoding | Fail safely; never accept another state |
| 7-day weather | Live | Open-Meteo | Real in-session cache |
| Mandi price | Latest published observation | AGMARKNET / CEDA / data.gov.in | Real in-memory cache; optional user-verified manual value |
| Crop requirements | Reference | Java crop classes | Not described as live |
| Crop score | Calculated | Java decision logic | No AI decision |
| Sowing advice | Calculated | Java + weather | No fabricated weather |
| Profit | Calculated estimate | Inputs + genuine mandi evidence | Not calculated silently with fake data |
| EMI/affordability | Calculated | Java finance formulas | Never called bank approval |
| Friendly narration | Formatter | Gemini or local Hinglish narrator | Local narrator |

No runtime CSV/TXT database is used for farmer or equipment persistence.
