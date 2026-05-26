package com.example.ibe.service;

import com.example.ibe.dto.CalendarDayPrice;
import com.example.ibe.exception.BadRequestException;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.exception.PropertyNotFoundException;
import com.example.ibe.repository.BookingAvailabilityRepository;
import com.example.ibe.repository.DailyPriceProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

        @Mock
        private BookingAvailabilityRepository bookingAvailabilityRepository;

        @Mock
        private ValidationService validationService;

        @InjectMocks
        private CalendarService calendarService;

        @Test
        void getCalendarReturnsCalendarRowsWithDefaultFallbackPrice() {
                UUID propertyId = UUID.randomUUID();
                LocalDate today = LocalDate.now();

                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
                doNothing().when(validationService).validatePropertyExists(propertyId);
                DailyPriceProjection mockProj = new DailyPriceProjection() {
                        @Override
                        public LocalDate getDate() {
                                return today;
                        }

                        @Override
                        public BigDecimal getMinPrice() {
                                return new BigDecimal("200.00");
                        }
                };
                when(bookingAvailabilityRepository.findMinPricePerDay(eq(propertyId), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(Collections.singletonList(mockProj));

                List<CalendarDayPrice> result = calendarService.getCalendar(propertyId.toString());

                assertNotNull(result);
                assertTrue(result.size() >= 60);
                assertEquals(today, result.get(0).getDate());
                assertEquals(new BigDecimal("200.00"), result.get(0).getMinPrice());
        }

        @Test
        void getCalendarThrowsWhenPropertyNotFound() {
                UUID propertyId = UUID.randomUUID();
                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
                doThrow(new PropertyNotFoundException("Property not found: " + propertyId))
                                .when(validationService)
                                .validatePropertyExists(propertyId);

                PropertyNotFoundException ex = assertThrows(
                                PropertyNotFoundException.class,
                                () -> calendarService.getCalendar(propertyId.toString()));

                assertTrue(ex.getMessage().contains("Property not found"));
        }

        @Test
        void getCalendarThrowsWhenValidatePropertyAccessFails() {
                UUID propertyId = UUID.randomUUID();
                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);

                doThrow(new DataProcessingException(
                                "Failed to validate property",
                                new DataRetrievalFailureException("db error")))
                                .when(validationService)
                                .validatePropertyExists(propertyId);

                DataProcessingException ex = assertThrows(
                                DataProcessingException.class,
                                () -> calendarService.getCalendar(propertyId.toString()));

                assertTrue(ex.getMessage().contains("Failed to validate property"));
        }

        @Test
        void getCalendarThrowsWhenCalendarFetchFails() {
                UUID propertyId = UUID.randomUUID();

                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
                doNothing().when(validationService).validatePropertyExists(propertyId);
                when(bookingAvailabilityRepository.findMinPricePerDay(eq(propertyId), any(LocalDate.class), any(LocalDate.class)))
                                .thenThrow(new DataRetrievalFailureException("db error"));

                DataProcessingException ex = assertThrows(
                                DataProcessingException.class,
                                () -> calendarService.getCalendar(propertyId.toString()));

                assertTrue(ex.getMessage().contains("Failed to fetch calendar prices"));
        }

        @Test
        void getMinPriceForDateRangeReturnsZeroWhenRepositoryReturnsNull() {
                UUID propertyId = UUID.randomUUID();
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(2);

                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
                doNothing().when(validationService).validatePropertyExists(propertyId);
                when(bookingAvailabilityRepository.findMinPriceForFullStay(propertyId, startDate, endDate))
                                .thenReturn(null);

                BigDecimal result = calendarService.getMinPriceForDateRange(propertyId.toString(), startDate, endDate);

                assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void getMinPriceForDateRangeThrowsWhenStartAfterEnd() {
                UUID propertyId = UUID.randomUUID();
                LocalDate startDate = LocalDate.now().plusDays(3);
                LocalDate endDate = LocalDate.now();

                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
                BadRequestException ex = assertThrows(
                                BadRequestException.class,
                                () -> calendarService.getMinPriceForDateRange(propertyId.toString(), startDate, endDate));

                assertTrue(ex.getMessage().contains("startDate must be before or equal to endDate"));
        }

        @Test
        void getMinPriceForDateRangeThrowsWhenRepositoryFails() {
                UUID propertyId = UUID.randomUUID();
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(2);

                when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
                doNothing().when(validationService).validatePropertyExists(propertyId);
                when(bookingAvailabilityRepository.findMinPriceForFullStay(propertyId, startDate, endDate))
                                .thenThrow(new DataRetrievalFailureException("db error"));

                DataProcessingException ex = assertThrows(
                                DataProcessingException.class,
                                () -> calendarService.getMinPriceForDateRange(propertyId.toString(), startDate, endDate));

                assertTrue(ex.getMessage().contains("Failed to fetch min price for date range"));
        }
}
