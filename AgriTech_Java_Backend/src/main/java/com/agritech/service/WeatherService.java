package com.agritech.service;

import com.agritech.model.DailyForecast;
import java.util.List;

public interface WeatherService {
    List<DailyForecast> getSevenDayForecast(String district, String state);
}
