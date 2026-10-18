package com.agritech.service;

import com.agritech.model.DailyForecast;
import java.util.List;

public interface WeatherService {
    List<DailyForecast> getSevenDayForecast(String district, String state);
    default com.agritech.model.WeatherReport getReport(String village,String district,String state){return new com.agritech.model.WeatherReport(getSevenDayForecast(district,state),district+", "+state,"Unknown",java.time.Instant.now(),false);}
}
