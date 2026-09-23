package com.archguard.api.service;

/** Calculates the deterministic health score shown for a completed scan. */
public final class HealthScoreCalculator {
    private static final int PERFECT_SCORE = 100;
    private static final int PENALTY_PER_VIOLATION = 10;

    private HealthScoreCalculator() {
    }

    public static int calculate(long violationCount) {
        if (violationCount < 0) {
            throw new IllegalArgumentException("Violation count cannot be negative");
        }
        return (int) Math.max(0, PERFECT_SCORE - PENALTY_PER_VIOLATION * violationCount);
    }
}
