package com.brein.time.utils;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public enum TimeModifier {
    START_OF_MINUTE,
    START_OF_HOUR,
    START_OF_DAY,
    END_OF_DAY,
    // Weeks start on Sunday
    START_OF_WEEK,
    START_OF_MONTH,
    NONE;

    /**
     * Changes the timestamp by adding (positive {@code days}) or removing (negative {@code days}) days from the
     * specified timestamp.
     *
     * @param timestamp the timestamp to start from
     * @param normalize {@code true} to normalize the date to the start of the day, otherwise {@code false}
     * @param days      the days to be added or removed
     *
     * @return the modified timestamp
     */
    public static long moveDays(final long timestamp, final boolean normalize, final long days) {
        return moveDays(timestamp, TimeUtils.UTC, normalize, days);
    }

    public static long moveDays(final long timestamp,
                                final String zoneId,
                                final boolean normalize,
                                final long days) {
        return moveDays(timestamp, TimeUtils.zoneId(zoneId), normalize, days);
    }

    public static long moveDays(final long timestamp,
                                final ZoneId zoneId,
                                final boolean normalize,
                                final long days) {
        final ZonedDateTime dateTime = TimeUtils.toZone(timestamp, zoneId);
        final ZonedDateTime normDateTime = normalize ? START_OF_DAY.applyModifier(dateTime) : dateTime;

        return normDateTime.plusDays(days).toEpochSecond();
    }

    /**
     * Changes the timestamp by adding (positive {@code months}) or removing (negative {@code months}) months from the
     * specified timestamp.
     *
     * @param timestamp the timestamp to start from
     * @param normalize {@code true} to normalize the date to the start of the month, otherwise {@code false}
     * @param months    the months to be added or removed
     *
     * @return the modified timestamp
     */
    public static long moveMonths(final long timestamp, final boolean normalize, final long months) {
        return moveMonths(timestamp, TimeUtils.UTC, normalize, months);
    }

    public static long moveMonths(final long timestamp,
                                  final String zoneId,
                                  final boolean normalize,
                                  final long days) {
        return moveMonths(timestamp, TimeUtils.zoneId(zoneId), normalize, days);
    }

    public static long moveMonths(final long timestamp,
                                  final ZoneId zoneId,
                                  final boolean normalize,
                                  final long months) {
        final ZonedDateTime dateTime = TimeUtils.toZone(timestamp, zoneId);
        final ZonedDateTime normDateTime = normalize ? START_OF_MONTH.applyModifier(dateTime) : dateTime;

        return normDateTime.plusMonths(months).toEpochSecond();
    }

    public ZonedDateTime applyModifier(final ZonedDateTime dateTime) {
        if (START_OF_MINUTE.equals(this)) {
            return dateTime.truncatedTo(ChronoUnit.MINUTES);
        } else if (START_OF_HOUR.equals(this)) {
            return dateTime.truncatedTo(ChronoUnit.HOURS);
        } else if (START_OF_DAY.equals(this)) {
            return dateTime.truncatedTo(ChronoUnit.DAYS);
        } else if (END_OF_DAY.equals(this)) {
            return dateTime
                    .truncatedTo(ChronoUnit.DAYS)
                    .plusDays(1)
                    .minusSeconds(1);
        } else if (START_OF_WEEK.equals(this)) {
            return dateTime.minusDays(dateTime.getDayOfWeek().getValue() % 7).truncatedTo(ChronoUnit.DAYS);
        } else if (START_OF_MONTH.equals(this)) {
            return dateTime
                    .withDayOfMonth(1)
                    .with(LocalTime.of(0, 0));
        } else if (NONE.equals(this)) {
            return dateTime;
        } else {
            throw new IllegalArgumentException("Unexpected TimeModifier: " + this);
        }
    }

    /**
     * A unix timestamp to apply the modification to.
     *
     * @param timestamp the timestamp to apply the modification to
     *
     * @return the modified unix timestamp
     */
    public long applyModifier(final long timestamp) {
        return applyModifier(timestamp, TimeUtils.UTC);
    }

    public long applyModifier(final long timestamp,
                              final ZoneId zoneId) {
        return applyModifier(TimeUtils.toZone(timestamp, zoneId)).toEpochSecond();
    }
}
