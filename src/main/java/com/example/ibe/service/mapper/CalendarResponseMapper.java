package com.example.ibe.service.mapper;

import com.example.ibe.dto.CalendarDayPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CalendarResponseMapper {

    private CalendarResponseMapper() {
    }

    public static List<CalendarDayPrice> toCalendarDays(
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, BigDecimal> priceMap) {
        List<CalendarDayPrice> calendarDays = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            BigDecimal price = priceMap.getOrDefault(currentDate, BigDecimal.ZERO);

            calendarDays.add(
                    CalendarDayPrice.builder()
                            .date(currentDate)
                            .minPrice(price)
                            .build());

            currentDate = currentDate.plusDays(1);
        }

        return calendarDays;
    }
}