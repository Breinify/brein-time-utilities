package com.brein.time.utils;

/**
 * Enum representing the type of day-of-month selection.
 * <p>
 * This allows rules to be expressed in a human-readable way, such as "first N days",
 * "last N days", "specific days", or a range of days.
 */
public enum DaySelectorType {

    /**
     * Selects the first N days of the month.
     * <p>
     * Example: {@code Collections.singletonList(7)} selects days 1 through 7 (inclusive).
     * The list should contain a single element specifying N.
     */
    FIRST_N_DAYS,

    /**
     * Selects the last N days of the month.
     * <p>
     * Example: {@code Collections.singletonList(5)} selects the last 5 days of the month.
     * The list should contain a single element specifying N.
     */
    LAST_N_DAYS,

    /**
     * Selects specific days of the month explicitly.
     * <p>
     * Example: {@code List.of(1, 3, 15, 27)} selects exactly these days.
     * Each day must be in the range 1..monthLength.
     */
    SPECIFIC_DAYS,

    /**
     * Selects a continuous range of days of the month.
     * <p>
     * Example: {@code List.of(10, 15)} selects days 10 through 15 (inclusive start, inclusive end).
     * The list should contain exactly two elements: startDay and endDay.
     */
    RANGE
}