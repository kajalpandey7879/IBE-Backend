package com.example.ibe.controller;

import com.example.ibe.dto.CalendarDayPrice;
import com.example.ibe.service.CalendarService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Validated
public class CalendarQueryResolver {
    private final CalendarService calendarService;

    @QueryMapping
    public List<CalendarDayPrice> calendar(
            @Argument("propertyId") @NotBlank(message = "propertyId must not be blank") String propertyId) {
        return calendarService.getCalendar(propertyId);
    }

    @QueryMapping
    public BigDecimal minPriceForDateRange(
            @Argument("propertyId") @NotBlank(message = "propertyId must not be blank") String propertyId,
            @Argument("startDate") @NotNull(message = "startDate must not be null") LocalDate startDate,
            @Argument("endDate") @NotNull(message = "endDate must not be null") LocalDate endDate) {
        return calendarService.getMinPriceForDateRange(propertyId, startDate, endDate);
    }
}
