package com.archguard.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HealthScoreCalculatorTest {
    @Test
    void deductsTenPointsPerViolation() {
        assertEquals(70, HealthScoreCalculator.calculate(3));
    }

    @Test
    void neverReturnsLessThanZero() {
        assertEquals(0, HealthScoreCalculator.calculate(11));
    }

    @Test
    void rejectsNegativeViolationCounts() {
        assertThrows(IllegalArgumentException.class, () -> HealthScoreCalculator.calculate(-1));
    }
}
