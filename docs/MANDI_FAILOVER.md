# Mandi data failover

The application never fabricates a market price.

Provider order:
1. AGMARKNET 2.0 public reporting API
2. CEDA Agri Market public interface (DMI/AGMARKNET-derived latest-available data)
3. data.gov.in / AGMARKNET resource using DATA_GOV_API_KEY
4. Option 4 only: optional user-entered VERIFIED current mandi price if every network source is unavailable

Every displayed market record includes a source and data date. State-wide fallbacks are labelled explicitly.
