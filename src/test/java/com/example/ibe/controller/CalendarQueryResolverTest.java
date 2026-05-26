package com.example.ibe.controller;

import com.example.ibe.dto.CalendarDayPrice;
import com.example.ibe.exception.InvalidUuidFormatException;
import com.example.ibe.service.CalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarQueryResolverTest {

    @Mock
    private CalendarService calendarService;

    @InjectMocks
    private CalendarQueryResolver resolver;

    @Test
    void calendarThrowsWhenPropertyIdIsInvalidUuid() {
        when(calendarService.getCalendar("not-a-uuid"))
                .thenThrow(new InvalidUuidFormatException("propertyId must be a valid UUID", new IllegalArgumentException()));
        InvalidUuidFormatException ex = assertThrows(
                InvalidUuidFormatException.class,
                () -> resolver.calendar("not-a-uuid"));

        assertEquals("propertyId must be a valid UUID", ex.getMessage());
    }

    @Test
    void minPriceForDateRangeThrowsWhenPropertyIdIsInvalidUuid() {
        when(calendarService.getMinPriceForDateRange(eq("bad-uuid"), any(), any()))
                .thenThrow(new InvalidUuidFormatException("propertyId must be a valid UUID", new IllegalArgumentException()));
        InvalidUuidFormatException ex = assertThrows(
                InvalidUuidFormatException.class,
                () -> resolver.minPriceForDateRange("bad-uuid", LocalDate.now(), LocalDate.now().plusDays(1)));

        assertEquals("propertyId must be a valid UUID", ex.getMessage());
    }

    @Test
    void calendarDelegatesToServiceForValidUuid() {
        UUID propertyId = UUID.randomUUID();
        CalendarDayPrice day = CalendarDayPrice.builder()
                .date(LocalDate.now())
                .minPrice(new BigDecimal("120.00"))
                .build();

        when(calendarService.getCalendar(propertyId.toString())).thenReturn(List.of(day));

        List<CalendarDayPrice> response = resolver.calendar(propertyId.toString());

        assertEquals(1, response.size());
        verify(calendarService).getCalendar(propertyId.toString());
    }

    @Test
    void minPriceForDateRangeDelegatesToServiceForValidUuid() {
        UUID propertyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(2);

        when(calendarService.getMinPriceForDateRange(propertyId.toString(), startDate, endDate))
                .thenReturn(new BigDecimal("200.00"));

        BigDecimal response = resolver.minPriceForDateRange(propertyId.toString(), startDate, endDate);

        assertEquals(new BigDecimal("200.00"), response);
        verify(calendarService).getMinPriceForDateRange(propertyId.toString(), startDate, endDate);
    }
}
