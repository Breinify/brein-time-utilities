package com.brein.time.utils;

import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;

public class TestTimeUtils {

    @Test
    public void testFirstOfLastMonth() {
        // 07/01/2016
        final long unixTimestamp = 1467331200;

        // Sometime in august
        final long augustTime = 1470787200;
        Assert.assertEquals(unixTimestamp, TimeUtils.firstOfLastMonthTime(augustTime));
        Assert.assertTrue(TimeUtils.isSameMonth(TimeUtils.firstOfLastMonthTime(augustTime), unixTimestamp));
    }

    @Test
    public void testValidateDate() {
        Assert.assertTrue(TimeUtils.validateDate("11", "05", "2011"));
        Assert.assertTrue(TimeUtils.validateDate("1", "1", "1990"));

        Assert.assertFalse(TimeUtils.validateDate("11", null, null));
        Assert.assertFalse(TimeUtils.validateDate("32", "11", "2010"));
    }

    @Test
    public void testFormat() {
        Assert.assertEquals("Tue Nov 29 2016 11:11:30 GMT-0800 (PST)",
                TimeUtils.format("E MMM dd yyyy HH:mm:ss 'GMT'Z (z)", 1480446690L, "America/Los_Angeles"));
    }

    @Test
    public void testDateStringToUnixTimestamp() {
        Assert.assertEquals(1480446690L, TimeUtils.dateStringToUnixTimestamp("2016-11-29 11:11:30",
                "yyyy-MM-dd HH:mm:ss", "America/Los_Angeles"));

        Assert.assertEquals(1480447581L, TimeUtils.dateStringToUnixTimestamp("2016-11-30 04:26:21",
                "yyyy-MM-dd HH:mm:ss", "Asia/Seoul"));
        Assert.assertEquals(1480447581L, TimeUtils.dateStringToUnixTimestamp("2016-11-30 4:26:21",
                "yyyy-MM-dd H:mm:ss", "Asia/Seoul"));
        Assert.assertEquals(1480447581L, TimeUtils.dateStringToUnixTimestamp("2016-11-30 04:26:21",
                "yyyy-MM-dd H:mm:ss", "Asia/Seoul"));
        /*
         * The following fails on Unix (not on Mac):
         *
         * Assert.assertEquals(1480447581L, TimeUtils.dateStringToUnixTimestamp("2016-11-30 4:26:21",
         *      "yyyy-MM-dd HH:mm:ss", "Asia/Seoul"));
         */

        Assert.assertEquals(1435019400L, TimeUtils.dateStringToUnixTimestamp("2015-06-22 17:30:00",
                "yyyy-MM-dd HH:mm:ss", "America/Los_Angeles"));
        Assert.assertEquals(1493148300L, TimeUtils.dateStringToUnixTimestamp("2017-04-25 15:25:00",
                "yyyy-MM-dd HH:mm:ss", "America/New_York"));
        Assert.assertEquals(1492348192L, TimeUtils.dateStringToUnixTimestamp("2017-04-16 08:09:52",
                "yyyy-MM-dd HH:mm:ss", "America/Chicago"));
        Assert.assertEquals(1492348192L, TimeUtils.dateStringToUnixTimestamp("2017-04-16 08:09:52",
                "yyyy-MM-dd HH:mm:ss", "America/Chicago"));

        Assert.assertEquals(1492348192L, TimeUtils.dateStringToUnixTimestamp("2017-4-16 8:09:52",
                "yyyy-M-d H:m:s", "America/Chicago"));

        Assert.assertEquals(1492348192L, TimeUtils.dateStringToUnixTimestamp("2017-04-16 08:09:52",
                "yyyy-M-d H:m:s", "America/Chicago"));

        Assert.assertEquals(1492348192L, TimeUtils.dateStringToUnixTimestamp("2017-04-16 8:9:52",
                "yyyy-M-d H:m:s", "America/Chicago"));
    }

    @Test
    public void testDateStringModification() {
        Assert.assertEquals(1480492799L, TimeUtils.dateStringModifyToUnixTimestamp("2016-11-29 11:11:30",
                "yyyy-MM-dd HH:mm:ss", "America/Los_Angeles", TimeModifier.END_OF_DAY));

        Assert.assertEquals(1480406400L, TimeUtils.dateStringModifyToUnixTimestamp("2016-11-29 11:11:30",
                "yyyy-MM-dd HH:mm:ss", "America/Los_Angeles", TimeModifier.START_OF_DAY));

        Assert.assertEquals(1480485599L, TimeUtils.dateStringModifyToUnixTimestamp("2016-11-29 11:11:30",
                "yyyy-MM-dd HH:mm:ss", "America/Chicago", TimeModifier.END_OF_DAY));

        Assert.assertEquals(1480399200L, TimeUtils.dateStringModifyToUnixTimestamp("2016-11-29 11:11:30",
                "yyyy-MM-dd HH:mm:ss", "America/Chicago", TimeModifier.START_OF_DAY));
    }

    @Test
    public void testTimeUtils() {
        // 07/01/2016
        final long unixTimestamp = 1467331200;

        // Sometime in august
        final long augustTime = 1470787200;
        Assert.assertEquals(unixTimestamp, TimeUtils.firstOfLastMonthTime(augustTime));
        Assert.assertTrue(TimeUtils.isSameMonth(TimeUtils.firstOfLastMonthTime(augustTime), unixTimestamp));
    }

    @Test
    public void testSecondsToFullMinute() {
        Assert.assertEquals(46, TimeUtils.secondsToFullMinute(1473887714L));
        Assert.assertEquals(0, TimeUtils.secondsToFullMinute(1473887700L));

        Assert.assertEquals(16, TimeUtils.secondsToFullMinute(1473887714L, 30));
        Assert.assertEquals(30, TimeUtils.secondsToFullMinute(1473887700L, 30));
        Assert.assertEquals(30, TimeUtils.secondsToFullMinute(1473887700L, 30));
        Assert.assertEquals(0, TimeUtils.secondsToFullMinute(1473887700L, 60));

        int secondCounter = 0;
        for (long i = 0; i < 10000; i++) {
            Assert.assertEquals((60 - secondCounter) % 60, TimeUtils.secondsToFullMinute(i));
            secondCounter = (secondCounter + 1) % 60;
        }
    }

    @Test
    public void testResolveUTC() {
        Assert.assertNotNull(TimeUtils.zoneId(TimeUtils.UTC.getId(), false));
    }

    @Test
    public void testZoneId() {
        Assert.assertNotNull(TimeUtils.zoneId(TimeUtils.UTC.getId(), false));
        Assert.assertNotNull(TimeUtils.zoneId("America/Chicago", false));
        Assert.assertNull(TimeUtils.zoneId("fake/timezone", false));
        Assert.assertNull(TimeUtils.zoneId("", false));
        Assert.assertNull(TimeUtils.zoneId(null, false));
    }

    @Test
    public void testSecondsToPrettyString() {
        Assert.assertEquals("0 seconds", TimeUtils.secondsToPrettyString(0));
        Assert.assertEquals("1 second", TimeUtils.secondsToPrettyString(1));
        Assert.assertEquals("10 seconds", TimeUtils.secondsToPrettyString(10));
        Assert.assertEquals("100 seconds", TimeUtils.secondsToPrettyString(100));
        Assert.assertEquals("17 minutes", TimeUtils.secondsToPrettyString(1000));

        Assert.assertEquals("3 hours", TimeUtils.secondsToPrettyString(10000));
        Assert.assertEquals("28 hours", TimeUtils.secondsToPrettyString(100000));
        Assert.assertEquals("12 days", TimeUtils.secondsToPrettyString(1000000));
        Assert.assertEquals("17 weeks", TimeUtils.secondsToPrettyString(10000000));
        Assert.assertEquals("3 years", TimeUtils.secondsToPrettyString(100000000));

        Assert.assertEquals("0 seconds", TimeUtils.secondsToPrettyString(-0));
        Assert.assertEquals("-1 second", TimeUtils.secondsToPrettyString(-1));
        Assert.assertEquals("-10 seconds", TimeUtils.secondsToPrettyString(-10));
        Assert.assertEquals("-100 seconds", TimeUtils.secondsToPrettyString(-100));
        Assert.assertEquals("-17 minutes", TimeUtils.secondsToPrettyString(-1000));

        Assert.assertEquals("-3 hours", TimeUtils.secondsToPrettyString(-10000));
        Assert.assertEquals("-28 hours", TimeUtils.secondsToPrettyString(-100000));
        Assert.assertEquals("-12 days", TimeUtils.secondsToPrettyString(-1000000));
        Assert.assertEquals("-17 weeks", TimeUtils.secondsToPrettyString(-10000000));
        Assert.assertEquals("-3 years", TimeUtils.secondsToPrettyString(-100000000));

        Assert.assertEquals("119 seconds", TimeUtils.secondsToPrettyString(119));
        Assert.assertEquals("2 minutes", TimeUtils.secondsToPrettyString(120));
        Assert.assertEquals("2 minutes", TimeUtils.secondsToPrettyString(121));
    }

    @Test
    public void testCreateTimestampList() {
        Assert.assertEquals(Collections.emptyList(), TimeUtils.createTimestampList(1L, 0L));

        Assert.assertEquals(Collections.singletonList(1497225600L),
                TimeUtils.createTimestampList(1497226525L, 1497226525L));

        Assert.assertEquals(Collections.singletonList(1497225600L),
                TimeUtils.createTimestampList(1497226025L, 1497226525L));

        Assert.assertEquals(Arrays.asList(1497052800L, 1497139200L, 1497225600L, 1497312000L),
                TimeUtils.createTimestampList(1497126525L, 1497326525L));
    }

    @Test
    public void testSecondsAfterMidnight() {
        long val;

        // 19:42:54
        val = TimeUtils.getSecondsAfterMidnight(1549338174L, ZoneId.of("America/Los_Angeles"));
        Assert.assertEquals(19 * 60 * 60 + 42 * 60 + 54, val);

        // 22:42:54
        val = TimeUtils.getSecondsAfterMidnight(1549338174L, ZoneId.of("America/New_York"));
        Assert.assertEquals(22 * 60 * 60 + 42 * 60 + 54, val);
    }

    @Test
    public void testFromZoneToZone() {
        ZonedDateTime result;


        result = TimeUtils.fromZoneToZone(1577865600, ZoneId.of("America/Los_Angeles"), TimeUtils.UTC);
        Assert.assertEquals(1577836800L, result.toInstant().getEpochSecond());
    }
}
