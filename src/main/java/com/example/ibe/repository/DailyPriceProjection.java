package com.example.ibe.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyPriceProjection {
    LocalDate getDate();
    BigDecimal getMinPrice();
}
