package com.brein.time.utils;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static final ZoneId UTC = ZoneId.of("UTC");
    protected static final Map<String, ZoneId> ZONES = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(TimeUtils.class);

    // fill the zones
    static {
        ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .forEach(zoneId -> ZONES.put(zoneId.getId().toLowerCase(), zoneId));
    }

    private TimeUtils() {
        /*
         * Utility classes, which are a collection of static members,
         * are not meant to be instantiated.
         */
    }

    public static String format(final long unixTimeStamp) {
        return formatUnixTimeStamp("yyyy-MM-dd HH:mm:ss z", unixTimeStamp);
    }

    public static String formatUnixTimeStamp(final long unixTimeStamp) {
        return formatUnixTimeStamp("yyyy/MM/dd HH:mm:ss", unixTimeStamp);
    }

    public static String formatUnixTimeStamp(final String format, final long unixTimeStamp) {
        return format(format, unixTimeStamp, UTC);
    }

    public static String format(final String format, final long unixTimeStamp, final String zone) {
        return format(format, unixTimeStamp, zoneId(zone));
    }

    public static String format(final String format, final long unixTimeStamp, final ZoneId zone) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return Instant.ofEpochSecond(unixTimeStamp)
                .atZone(zone)
                .format(formatter);
    }

    public static long now() {
        return Instant.now().getEpochSecond();
    }

    public static long nowMillis() {
        return Instant.now().toEpochMilli();
    }

    public static int secondsToFullMinute(final long now) {
        return secondsToFullMinute(now, 0);
    }

    /**
     * Calculate the rest of seconds to a full minute, i.e. at 10:44:14 the method returns 46.
     *
     * @param now the current time
     *
     * @return the seconds until the full minute
     */
    public static int secondsToFullMinute(final long now, final int offset) {
        final int value = Long.valueOf(60 - ((now + offset) % 60)).intValue();
        return value == 60 ? 0 : value;
    }

    public static Calendar createFirstOfMonthCal(final long unixTimestamp) {
        /*
         * Calendar set method doesn't know that Calendar.get(...) will return one of the months it expects. Thus, we
         * have to ignore the evaluation of MagicConstant.
         */
        final Calendar nowCalendar = createCal(unixTimestamp);
        final Calendar firstOfMonthCal = createCal();
        //noinspection MagicConstant
        firstOfMonthCal.set(nowCalendar.get(Calendar.YEAR), nowCalendar.get(Calendar.MONTH), 1, 0, 0, 0);

        return firstOfMonthCal;
    }

    public static Calendar createCal() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    public static Calendar createCal(final long unixTimeStamp) {
        final Calendar cal = createCal();
        cal.setTimeInMillis(unixTimeStamp * 1000L);

        return cal;
    }

    /**
     * @deprecated please use {@link TimeModifier#START_OF_MONTH}
     */
    @Deprecated
    public static long firstOfNextMonthTime(final long unixTimestamp) {
        final Calendar cal = createFirstOfMonthCal(unixTimestamp);
        cal.add(Calendar.MONTH, 1);

        return cal.getTimeInMillis() / 1000L;
    }

    /**
     * @param unixTimestamp Current time
     *
     * @return Timestamp for the beginning of the previous month relative to current time
     */
    public static long firstOfLastMonthTime(final long unixTimestamp) {
        final Calendar cal = createFirstOfMonthCal(unixTimestamp);
        cal.add(Calendar.MONTH, -1);

        return cal.getTimeInMillis() / 1000L;
    }

    public static long firstOfCurrentMonthTime(final long unixTimestamp) {
        final Calendar cal = createFirstOfMonthCal(unixTimestamp);
        return cal.getTimeInMillis() / 1000L;
    }

    public static boolean isSameMonth(final long unixTimestamp1,
                                      final long unixTimestamp2) {
        final Calendar cal1 = createCal(unixTimestamp1);
        final Calendar cal2 = createCal(unixTimestamp2);
        return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR))
                && (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH));
    }

    public static boolean validateDate(final String day,
                                       final String month,
                                       final String year) {

        if (day == null || month == null || year == null) {
            return false;
        }

        final String format = "dd-MM-uuuu";
        final String date = String.format("%02d-%02d-%s", Integer.parseInt(day), Integer.parseInt(month), year);

        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern(format).withResolverStyle(ResolverStyle.STRICT));
            return true;
        } catch (final DateTimeParseException e) {
            return false;
        }
    }

    public static ZonedDateTime toZone(final long utc, final String zoneId) {
        return toZone(utc, zoneId(zoneId));
    }

    public static ZonedDateTime toZone(final long utc, final ZoneId toZone) {
        final Instant instant = Instant.ofEpochSecond(utc);
        final ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, UTC);

        return toZone(zdt, toZone);
    }

    public static ZonedDateTime toZone(final ZonedDateTime utc, final String zoneId) {
        return toZone(utc, zoneId(zoneId));
    }

    public static ZonedDateTime toZone(final ZonedDateTime utc, final ZoneId toZone) {
        if (!UTC.equals(utc.getZone())) {
            throw new IllegalArgumentException("Expecting UTC time.");
        }

        return utc.withZoneSameInstant(toZone);
    }

    public static ZonedDateTime fromZoneToUtc(final long ts, final ZoneId zone) {
        return fromZoneToZone(ts, zone, UTC);
    }

    public static ZonedDateTime fromZoneToUtc(final ZonedDateTime ts,
                                              final ZoneId zone) {
        return fromZoneToZone(ts, zone, UTC);
    }

    public static ZonedDateTime fromZoneToZone(final long ts, final ZoneId zone, final ZoneId toZone) {
        return fromZoneToZone(ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts), zone), zone, toZone);
    }

    public static ZonedDateTime fromZoneToZone(final ZonedDateTime ts,
                                               final ZoneId zone,
                                               final ZoneId toZone) {
        return ts.withZoneSameLocal(toZone);
    }

    public static ZoneId zoneId(final String tzId) {
        return zoneId(tzId, true);
    }

    public static ZoneId zoneId(final String tzId, final boolean caseSensitive) {
        if (tzId == null) {
            return null;
        } else if (caseSensitive) {
            try {
                return ZoneId.of(tzId);
            } catch (final Exception e) {
                LOGGER.warn("Problem looking up time zone id " + tzId, e);
                return null;
            }
        } else {
            return ZONES.get(tzId.toLowerCase());
        }
    }

    /**
     * Converts a time to it's midnight
     *
     * @param time unix-timestamp (in seconds)
     *
     * @return the midnight's time
     *
     * @deprecated since 1.6.2 use {@link TimeModifier#START_OF_DAY} instead
     */
    @Deprecated()
    public static long toMidnight(final long time) {
        return TimeModifier.START_OF_DAY.applyModifier(time);
    }

    public static long getSecondsAfterMidnight(final long unixTimestamp,
                                               final String timezone) {
        final ZonedDateTime zone = toZone(unixTimestamp, timezone);
        return getSecondsAfterMidnight(zone);
    }

    public static long getSecondsAfterMidnight(final long unixTimestamp,
                                               final ZoneId toZone) {
        final ZonedDateTime zone = toZone(unixTimestamp, toZone);
        return getSecondsAfterMidnight(zone);
    }

    public static long getSecondsAfterMidnight(final ZonedDateTime zonedDateTime) {
        final ZonedDateTime midnight = TimeModifier.START_OF_DAY.applyModifier(zonedDateTime);
        return ChronoUnit.SECONDS.between(midnight, zonedDateTime);
    }

    /**
     * Converts specified date string with given format to a unix timestamp. Returns -1 if there was a failure
     *
     * @param dateString Date string value
     * @param format     Format that date string is in
     *
     * @return Unix timestamp, or -1 if something went wrong
     */
    public static long dateStringToUnixTimestamp(final String dateString,
                                                 final String format) {
        return dateStringToUnixTimestamp(dateString, format, UTC);
    }

    public static long dateStringToUnixTimestamp(final String dateString,
                                                 final String format,
                                                 final String timezone) {
        return dateStringToUnixTimestamp(dateString, DateTimeFormatter.ofPattern(format), timezone);
    }

    public static long dateStringToUnixTimestamp(final String dateString,
                                                 final String format,
                                                 final ZoneId zone) {
        return dateStringToUnixTimestamp(dateString, DateTimeFormatter.ofPattern(format), zone);
    }

    public static long dateStringToUnixTimestamp(final String dateString,
                                                 final DateTimeFormatter formatter,
                                                 final String timezone) {
        return dateStringToUnixTimestamp(dateString, formatter, getZone(timezone));
    }

    public static long dateStringToUnixTimestamp(final String dateString,
                                                 final DateTimeFormatter formatter,
                                                 final ZoneId zone) {
        if (dateString == null || dateString.isEmpty()) {
            return -1;
        }

        try {
            return LocalDateTime.parse(dateString, formatter)
                    .atZone(zone)
                    .toEpochSecond();
        } catch (final DateTimeParseException e) {
            LOGGER.error("Unable to parse date '" + dateString + "'", e);
            return -1;
        }
    }

    public static long dateStringModifyToUnixTimestamp(final String dateString,
                                                       final String format,
                                                       final String timezone,
                                                       final TimeModifier modifier) {
        return dateStringModifyToUnixTimestamp(dateString, DateTimeFormatter.ofPattern(format), timezone, modifier);
    }

    public static long dateStringModifyToUnixTimestamp(final String dateString,
                                                       final DateTimeFormatter formatter,
                                                       final String timezone,
                                                       final TimeModifier modifier) {
        if (dateString == null || dateString.isEmpty()) {
            return -1;
        }

        try {
            ZonedDateTime dateTime = LocalDateTime.parse(dateString, formatter)
                    .atZone(getZone(timezone));

            if (modifier != null) {
                dateTime = modifier.applyModifier(dateTime);
            }

            return dateTime.toEpochSecond();
        } catch (final DateTimeParseException e) {
            LOGGER.error("Unable to parse date '" + dateString + "'");
            return -1;
        }
    }

    public static String convertDateFormat(final String dateString,
                                           final String fromFormat,
                                           final String toFormat) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(fromFormat);
        try {
            final Date date = dateFormat.parse(dateString);
            dateFormat.applyPattern(toFormat);
            return dateFormat.format(date);
        } catch (final ParseException e) {
            LOGGER.error("Unable to parse date format: " + fromFormat);
            return null;
        }
    }

    public static String secondsToPrettyString(final long seconds) {
        if (seconds < 0) {
            return "-" + secondsToPrettyString(-seconds);
        } else {
            final double converted;
            final String type;
            if (seconds >= 2 * 365 * 24 * 60 * 60) {
                converted = seconds / 365 / 24 / 60 / 60.0;
                type = "years";
            } else if (seconds >= 2 * 7 * 24 * 60 * 60) {
                converted = seconds / 7 / 24 / 60 / 60.0;
                type = "weeks";
            } else if (seconds >= 2 * 24 * 60 * 60) {
                converted = seconds / 24 / 60 / 60.0;
                type = "days";
            } else if (seconds >= 2 * 60 * 60) {
                converted = seconds / 60 / 60.0;
                type = "hours";
            } else if (seconds >= 2 * 60) {
                converted = seconds / 60.0;
                type = "minutes";
            } else if (seconds == 1) {
                converted = 1;
                type = "second";
            } else {
                converted = seconds;
                type = "seconds";
            }
            return Math.round(converted) + " " + type;
        }
    }

    /**
     * Creates a list of days between the specified start (inclusive) and end (inclusive).
     *
     * @param startUnixTimestamp the start
     * @param endUnixTimestamp   the end
     *
     * @return the unix timestamps for each day between start and end
     */
    public static List<Long> createTimestampList(final long startUnixTimestamp,
                                                 final long endUnixTimestamp) {
        if (startUnixTimestamp > endUnixTimestamp) {
            return Collections.emptyList();
        }

        // normalize the start and end (next day's start)
        final long normStart = TimeModifier.START_OF_DAY.applyModifier(startUnixTimestamp);
        final long normEnd = TimeModifier.moveDays(endUnixTimestamp, true, 1);

        // determine which times we have to query for
        final List<Long> times = new ArrayList<>();
        for (long time = normStart; time < normEnd; time += 24 * 60 * 60) {
            times.add(time);
        }

        return times;
    }

    public static ChronoUnit convertToChronoUnit(final String chronoUnit) {
        if (chronoUnit == null) {
            return null;
        }

        try {
            return ChronoUnit.valueOf(chronoUnit.toUpperCase());
        } catch (final IllegalArgumentException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Couldn't get value of " + chronoUnit, e);
            }
            return null;
        }
    }

    public static TimeUnit convertToTimeUnit(final String timeUnit) {
        if (timeUnit == null) {
            return null;
        }

        try {
            return TimeUnit.valueOf(timeUnit.toUpperCase());
        } catch (final IllegalArgumentException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Couldn't get value of " + timeUnit, e);
            }
            return null;
        }
    }

    public static TimeUnit convert(final ChronoUnit chronoUnit) {
        if (chronoUnit == null) {
            return null;
        }

        switch (chronoUnit) {
            case DAYS:
                return TimeUnit.DAYS;
            case HOURS:
                return TimeUnit.HOURS;
            case MINUTES:
                return TimeUnit.MINUTES;
            case SECONDS:
                return TimeUnit.SECONDS;
            case MICROS:
                return TimeUnit.MICROSECONDS;
            case MILLIS:
                return TimeUnit.MILLISECONDS;
            case NANOS:
                return TimeUnit.NANOSECONDS;
            default:
                return null;
        }
    }

    public static ChronoUnit convert(final TimeUnit timeUnit) {
        if (timeUnit == null) {
            return null;
        }

        switch (timeUnit) {
            case DAYS:
                return ChronoUnit.DAYS;
            case HOURS:
                return ChronoUnit.HOURS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            default:
                return null;
        }
    }

    protected static ZoneId getZone(final String timezone) {
        return timezone == null ? UTC : ZoneId.of(timezone);
    }
}
