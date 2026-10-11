package com.agritech.service;

import com.agritech.model.DailyForecast;
import java.time.LocalDate;
import java.util.List;

public class DemoWeatherService implements WeatherService {
    public List<DailyForecast> getSevenDayForecast(String district, String state) {
        LocalDate d = LocalDate.now();
        return List.of(
                new DailyForecast(d,22,31,2,20,58,8),
                new DailyForecast(d.plusDays(1),21,29,28,88,76,14),
                new DailyForecast(d.plusDays(2),20,28,18,70,72,12),
                new DailyForecast(d.plusDays(3),20,27,5,35,65,8),
                new DailyForecast(d.plusDays(4),19,27,1,15,60,7),
                new DailyForecast(d.plusDays(5),19,28,0,10,57,6),
                new DailyForecast(d.plusDays(6),20,29,3,25,61,9)
        );
    }
}
