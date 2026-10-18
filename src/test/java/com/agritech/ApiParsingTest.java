package com.agritech;

import com.agritech.exception.ApiException;
import com.agritech.service.DataGovMandiService;
import com.agritech.service.AgmarknetMandiService;
import com.agritech.service.MandiService;
import com.agritech.service.ResilientMandiService;
import com.agritech.model.MandiPrice;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.agritech.service.OpenMeteoGeocodingService;
import com.agritech.service.OpenMeteoWeatherService;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

class ApiParsingTest {
    @Test
    void parsesLocationFixture() {
        var result = new OpenMeteoGeocodingService(HttpClient.newHttpClient()).parse(
                "{\"results\":[{\"name\":\"Mahesāna\",\"latitude\":23.6,\"longitude\":72.4,\"admin1\":\"Gujarat\",\"country\":\"India\"}]}"
        );
        assertEquals("Mahesana", result.resolvedPlaceName());
        assertEquals(23.6, result.latitude());
    }

    @Test
    void acceptsGujratAliasButRejectsWrongState() {
        var service = new OpenMeteoGeocodingService(HttpClient.newHttpClient());
        String gujarat = "{\"results\":[{\"name\":\"Mahesana\",\"latitude\":23.6,\"longitude\":72.4,\"admin1\":\"Gujarat\",\"admin2\":\"Mahesana\",\"country_code\":\"IN\",\"country\":\"India\"}]}";
        assertEquals("Gujarat", service.parse(gujarat, "Mehsana", "Gujrat").region());

        String rajasthan = "{\"results\":[{\"name\":\"Kherwa\",\"latitude\":26.0,\"longitude\":74.0,\"admin1\":\"Rajasthan\",\"admin2\":\"Pali\",\"country_code\":\"IN\",\"country\":\"India\"}]}";
        assertThrows(ApiException.class, () -> service.parse(rajasthan, "Mehsana", "Gujarat"));
    }

    @Test
    void parsesWeatherFixture() {
        String json = "{\"daily\":{\"time\":[\"2026-10-11\"],\"temperature_2m_min\":[20],\"temperature_2m_max\":[30],\"precipitation_sum\":[2],\"precipitation_probability_max\":[25],\"relative_humidity_2m_mean\":[65],\"wind_speed_10m_max\":[10]}}";
        var days = new OpenMeteoWeatherService((v, d, s) -> null, HttpClient.newHttpClient()).parse(json);
        assertEquals(1, days.size());
        assertEquals(25, days.get(0).rainProbability());
    }

    @Test
    void parsesMandiFixtureAndMehsanaAlias() {
        String json = "{\"records\":[{\"state\":\"Gujarat\",\"district\":\"Mahesana\",\"market\":\"Unjha\",\"commodity\":\"Wheat\",\"variety\":\"Local\",\"arrival_date\":\"10/10/2026\",\"min_price\":\"2200\",\"max_price\":\"2500\",\"modal_price\":\"2400\"}]}";
        var prices = new DataGovMandiService("x", HttpClient.newHttpClient()).parse(json, "Mehsana", false);
        assertEquals(2400, prices.get(0).modalPrice());
        assertTrue(prices.get(0).exactDistrict());
    }


    @Test
    void parsesOfficialAgmarknetFallbackFixture() {
        String json = "{\"markets\":[{\"marketName\":\"Mehsana APMC\",\"dates\":[{\"arrivalDate\":\"12/10/2026\",\"data\":[{\"variety\":\"Local\",\"minimumPrice\":2200,\"maximumPrice\":2550,\"modalPrice\":2400}]}]}]}";
        var prices = new AgmarknetMandiService(HttpClient.newHttpClient())
                .parseMonthResponse(json, "Wheat", "Gujarat", "Mehsana", false);
        assertEquals(1, prices.size());
        assertEquals(2400, prices.get(0).modalPrice());
        assertTrue(prices.get(0).exactDistrict());
        assertTrue(prices.get(0).source().contains("AGMARKNET 2.0"));
    }

    @Test
    void fallsBackWhenPrimaryMandiProviderIsRateLimited() {
        MandiService primary = (crop, state, district) -> {
            throw new ApiException("HTTP 429");
        };
        MandiPrice fallbackPrice = new MandiPrice(
                "Wheat", "Gujarat", "Gujarat (state-wide)", "Official APMC",
                2200, 2500, 2400, 0,
                LocalDate.of(2026, 10, 12), "Local",
                "AGMARKNET 2.0 (official fallback)", false, false, Instant.now()
        );
        MandiService fallback = (crop, state, district) -> List.of(fallbackPrice);

        MandiPrice result = new ResilientMandiService(primary, fallback)
                .bestPrice("Wheat", "Gujarat", "Mehsana");

        assertEquals(2400, result.modalPrice());
        assertTrue(result.source().contains("AGMARKNET 2.0"));
    }
}
