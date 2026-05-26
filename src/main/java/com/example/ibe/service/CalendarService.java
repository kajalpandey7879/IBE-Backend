package com.example.ibe.service;
import com.example.ibe.dto.CalendarDayPrice;
import com.example.ibe.exception.BadRequestException;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.repository.BookingAvailabilityRepository;
import com.example.ibe.repository.DailyPriceProjection;
import com.example.ibe.service.mapper.CalendarResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final BookingAvailabilityRepository bookingAvailabilityRepository;
    private final ValidationService validationService;

    /**
     * Fetches the lowest available price for each day in a rolling two-month window
     * starting from today for the given property.
     *
     * @param propertyId the property identifier to validate and query
     * @return the calendar day prices mapped for the requested property
     */
    public List<CalendarDayPrice> getCalendar(String propertyId) {
        UUID propertyUuid = validationService.parseUuid(propertyId, "propertyId");
        log.info("Fetching calendar prices for property: {}", propertyId);
        validationService.validatePropertyExists(propertyUuid);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(2);

        List<DailyPriceProjection> priceRows;
        try {
            priceRows = bookingAvailabilityRepository.findMinPricePerDay(propertyUuid, startDate, endDate);
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch calendar prices", ex);
        }

        Map<LocalDate, BigDecimal> priceMap = priceRows.stream()
                .collect(Collectors.toMap(
                        DailyPriceProjection::getDate,
                        DailyPriceProjection::getMinPrice));
        return CalendarResponseMapper.toCalendarDays(startDate, endDate, priceMap);
    }

    /**
     * Fetches the minimum total price for a full stay within the provided date range
     * after validating the property and requested dates.
     *
     * @param propertyId the property identifier to validate and query
     * @param startDate the requested check-in date
     * @param endDate the requested check-out date
     * @return the minimum stay price, or {@code BigDecimal.ZERO} when no price is available
     */
    public BigDecimal getMinPriceForDateRange(String propertyId, LocalDate startDate, LocalDate endDate) {
        UUID propertyUuid = validationService.parseUuid(propertyId, "propertyId");
        validationService.validateDateRangeStrict(startDate, endDate, "startDate", "endDate");
        log.info("Fetching minimum price for property {} from {} to {}", propertyId, startDate, endDate);
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate must be before or equal to endDate");
        }

        log.info("Fetching minimum price for property {} from {} to {}", propertyId, startDate, endDate);
        validationService.validatePropertyExists(propertyUuid);

        BigDecimal minPrice;
        try {
            minPrice = bookingAvailabilityRepository.findMinPriceForFullStay(propertyUuid, startDate, endDate);
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch min price for date range", ex);
        }

        return minPrice != null ? minPrice : BigDecimal.ZERO;
    }
}
