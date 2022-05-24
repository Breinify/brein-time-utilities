package com.brein.time.timeintervals.intervals;

import org.junit.Assert;
import org.junit.Test;

public class TestInterval {

    @Test
    public void testEqualAndCompare() {
        final IInterval<Long> interval = new LongInterval(1L, 5L);

        Assert.assertEquals(interval, interval);
        //noinspection EqualsWithItself
        Assert.assertEquals(0, interval.compareTo(interval));

        final NumberInterval<Long> interval1 = new LongInterval(null, null);
        Assert.assertEquals(1, interval.compareTo(interval1));
        Assert.assertEquals(-1, interval1.compareTo(interval));

        final NumberInterval<Long> interval2 = new LongInterval(1L, null);
        Assert.assertEquals(-1, interval.compareTo(interval2));
        Assert.assertEquals(1, interval2.compareTo(interval));

        final NumberInterval<Long> interval3 = new LongInterval(1L, 4L);
        Assert.assertEquals(1, interval.compareTo(interval3));
        Assert.assertEquals(-1, interval3.compareTo(interval));

        final NumberInterval<Long> interval4 = new LongInterval(1L, 5L);
        Assert.assertEquals(0, interval.compareTo(interval4));
        Assert.assertEquals(0, interval4.compareTo(interval));
        Assert.assertEquals(interval, interval4);
        Assert.assertEquals(interval4, interval);

        final NumberInterval<Long> interval5 = new NumberInterval<>(Long.class, 1L, 6L, false, true);
        Assert.assertEquals(0, interval.compareTo(interval5));
        Assert.assertEquals(0, interval5.compareTo(interval));
        Assert.assertEquals(interval, interval5);
        Assert.assertEquals(interval5, interval);

        final NumberInterval<Long> interval6 = new LongInterval(1L, 5L);
        Assert.assertEquals(0, interval6.compareTo(new LongInterval(1L, 5L)));
        Assert.assertEquals(0, interval6.compareTo(new DoubleInterval(1.0, 5.0)));
        Assert.assertEquals(1, interval6.compareTo(new DoubleInterval(0.9, 1.0)));
        Assert.assertEquals(-1, interval6.compareTo(new IntegerInterval(1, 6)));
        Assert.assertEquals(-1, interval6.compareTo(new NumberInterval<>(Long.class, 1L, 5L, true, true)));
        Assert.assertEquals(1, new NumberInterval<>(Long.class, 1L, 5L, true, true).compareTo(interval6));

        final NumberInterval<Double> interval7 = new DoubleInterval(1.0, 5.0, true, true);
        Assert.assertEquals(-1, interval7.compareTo(new LongInterval(1L, 5L, true, true)));
        Assert.assertEquals(1, new LongInterval(1L, 5L, true, true).compareTo(interval7));

        final IdInterval<String, Long> interval8 = new IdInterval<>("ID1", new LongInterval(1L, 5L));
        Assert.assertEquals(interval8, interval8);
        Assert.assertEquals(interval8, new IdInterval<>("ID1", new LongInterval(1L, 5L)));
        //noinspection AssertBetweenInconvertibleTypes
        Assert.assertNotEquals(interval8, new LongInterval(1L, 5L));

        Assert.assertEquals(-1, interval8.compareTo(new IdInterval<>("ID2", new LongInterval(1L, 5L))));
        Assert.assertEquals(1, new IdInterval<>("ID2", new LongInterval(1L, 5L)).compareTo(interval8));
    }

    @Test
    public void testOverlaps() {
        Assert.assertTrue(new LongInterval(1L, 10L)
                .overlaps(new IdInterval<>("TEST", new IntegerInterval(5, 100))));
        Assert.assertFalse(new LongInterval(1L, 10L)
                .overlaps(new IdInterval<>("TEST", new IntegerInterval(11, 100))));
        Assert.assertFalse(new LongInterval(1L, 10L).overlaps(new DoubleInterval(10.1, 10.2)));
        Assert.assertTrue(new LongInterval(1L, 10L).overlaps(new LongInterval(10L, 10L)));
        Assert.assertTrue(new LongInterval(1L, 10L).overlaps(new LongInterval(-10L, 12L)));

        Assert.assertTrue(new DoubleInterval(1.0, 5.0, true, true).overlaps(new DoubleInterval(1.0, 5.0, true, true)));
        Assert.assertFalse(new DoubleInterval(1.0, 4.9, true, true).overlaps(new DoubleInterval(4.9, 5.0, true, true)));
        Assert.assertTrue(new DoubleInterval(1.0, 4.9, true, false)
                .overlaps(new DoubleInterval(4.9, 5.0, false, true)));
    }

    @Test
    public void testIrOverlaps() {
        Assert.assertTrue(new LongInterval(1L, 10L).irOverlaps(new LongInterval(2L, 11L)));
        Assert.assertFalse(new LongInterval(null, null).irOverlaps(new LongInterval(1L, 1L)));
        Assert.assertFalse(new LongInterval(1L, 10L).irOverlaps(new LongInterval(2L, 10L)));
        Assert.assertFalse(new LongInterval(1L, 10L).irOverlaps(new LongInterval(1L, 11L)));
        Assert.assertFalse(new LongInterval(1L, 5L).irOverlaps(new LongInterval(8L, 10L)));

        Assert.assertFalse(new LongInterval(1L, 11L).irIsOverlappedBy(new LongInterval(1L, 10L)));
        Assert.assertTrue(new LongInterval(2L, 11L).irIsOverlappedBy(new LongInterval(1L, 10L)));
        Assert.assertFalse(new LongInterval(null, null).irIsOverlappedBy(new LongInterval(1L, 1L)));
    }

    @Test
    public void testIrEquals() {
        Assert.assertTrue(new LongInterval(null, null).irEquals(new LongInterval(null, null)));

        // see NumberInterval.md
        Assert.assertFalse(new DoubleInterval(null, null).irEquals(new LongInterval(null, null)));
        Assert.assertTrue(new IntegerInterval(5, 7).irEquals(new LongInterval(5L, 7L)));
        Assert.assertTrue(new DoubleInterval(1.0, 2.0).irEquals(new NumberInterval<>(Long.class, 0L, 2L, true, false)));
        Assert.assertFalse(new DoubleInterval(1.0, 2.0)
                .irEquals(new NumberInterval<>(Long.class, 0L, 2L, false, false)));
    }

    @Test
    public void testIrBegins() {
        Assert.assertTrue(new LongInterval(null, null).irBegins(new LongInterval(null, 8L)));
        Assert.assertFalse(new LongInterval(null, null).irBegins(new LongInterval(null, null)));
        Assert.assertTrue(new IntegerInterval(5, 7).irBegins(new LongInterval(5L, 6L)));
        Assert.assertFalse(new IntegerInterval(5, 7).irBegins(new LongInterval(5L, 8L)));
        Assert.assertTrue(new DoubleInterval(1.0, 2.0).irBegins(new LongInterval(0L, 2L, true, true)));
        Assert.assertFalse(new DoubleInterval(1.0, 2.0).irBegins(new LongInterval(0L, 1L, false, false)));
        Assert.assertFalse(new NumberInterval<>(Long.class, 0L, 2L, true, true).irBegins(new DoubleInterval(1.0, 2.0)));
        Assert.assertFalse(new LongInterval(5L, 6L).irBegins(new IntegerInterval(5, 7)));

        Assert.assertTrue(new LongInterval(null, 8L).irBeginsBy(new LongInterval(null, null)));
        Assert.assertFalse(new LongInterval(null, null).irBeginsBy(new LongInterval(null, null)));
        Assert.assertTrue(new LongInterval(0L, 2L, true, true).irBeginsBy(new DoubleInterval(1.0, 2.0)));
        Assert.assertTrue(new LongInterval(5L, 6L).irBeginsBy(new IntegerInterval(5, 7)));
        Assert.assertFalse(new DoubleInterval(1.0, 2.0).irBeginsBy(new LongInterval(0L, 1L, false, false)));
        Assert.assertTrue(new IntegerInterval(5, 7).irBeginsBy(new LongInterval(5L, 8L)));
    }

    @Test
    public void testIrEnds() {
        // see NumberInterval.md for further information
        Assert.assertFalse(new DoubleInterval(null, null).irEnds(new LongInterval(8L, null)));
        Assert.assertTrue(new LongInterval(null, null).irEnds(new LongInterval(8L, null)));
        Assert.assertFalse(new LongInterval(null, null).irEnds(new LongInterval(null, null)));

        Assert.assertTrue(new LongInterval(8L, null).irEndsBy(new LongInterval(null, null)));
    }

    @Test
    public void testStartsDirectlyBefore() {
        Assert.assertTrue(new LongInterval(8L, 9L).irStartsDirectlyBefore(new LongInterval(1L, 7L)));
        Assert.assertTrue(new NumberInterval<>(Double.class, 2.0, 3.0, true, true)
                .irStartsDirectlyBefore(new DoubleInterval(1.0, 2.0)));
        Assert.assertFalse(new NumberInterval<>(Double.class, 2.0, 3.0, false, true)
                .irStartsDirectlyBefore(new DoubleInterval(1.0, 2.0)));
    }

    @Test
    public void testEndsDirectlyBefore() {
        Assert.assertTrue(new LongInterval(null, 8L).irEndsDirectlyBefore(new IntegerInterval(9, null)));
        Assert.assertTrue(new DoubleInterval(1.0, 2.0, true, true).irEndsDirectlyBefore(new DoubleInterval(2.0, 5.0)));
        Assert.assertFalse(new DoubleInterval(1.0, 2.0, true, false)
                .irEndsDirectlyBefore(new DoubleInterval(2.0, 5.0)));
    }

    @Test
    public void testContains() {
        Assert.assertTrue(new LongInterval(1L, 10L).contains(5));
        Assert.assertTrue(new LongInterval(1L, 10L).contains(1.0));
        Assert.assertFalse(new LongInterval(1L, 10L).contains(10.1));
        Assert.assertTrue(new LongInterval(1L, 10L).contains(10.0));
        Assert.assertTrue(new LongInterval(1L, 10L).contains(10L));
        Assert.assertTrue(new LongInterval(1L, 10L).contains(10));
        Assert.assertTrue(new LongInterval(1L, 1L).contains(1L));
        Assert.assertFalse(new LongInterval(1L, 1L).contains(2L));
        Assert.assertFalse(new LongInterval(1L, 1L).contains(0L));
        Assert.assertFalse(new NumberInterval<>(Long.class, 1L, 2L, false, true).contains(2));
        Assert.assertTrue(new NumberInterval<>(Long.class, 1L, 2L, false, false).contains(2));
    }

    @Test
    public void testDetermineRelation() {
        Assert.assertEquals(AllenIntervalRelation.BEFORE, new LongInterval(1L, 5L).ir(new LongInterval(8L, 10L)));
        Assert.assertEquals(AllenIntervalRelation.AFTER, new LongInterval(8L, 10L).ir(new LongInterval(1L, 5L)));
        Assert.assertEquals(AllenIntervalRelation.EQUALS, new LongInterval(8L, 10L).ir(new LongInterval(8L, 10L)));
        Assert.assertEquals(AllenIntervalRelation.OVERLAPS, new LongInterval(1L, 5L).ir(new LongInterval(4L, 10L)));
        Assert.assertEquals(AllenIntervalRelation.IS_OVERLAPPED_BY,
                new LongInterval(4L, 10L).ir(new LongInterval(1L, 5L)));
        Assert.assertEquals(AllenIntervalRelation.ENDS_DIRECTLY_BEFORE,
                new LongInterval(1L, 5L).ir(new LongInterval(6L, 10L)));
        Assert.assertEquals(AllenIntervalRelation.STARTS_DIRECTLY_BEFORE,
                new LongInterval(6L, 10L).ir(new LongInterval(1L, 5L)));
    }

    @Test
    public void testUniqueIdentifier() {
        Assert.assertEquals("[1,2]", new LongInterval(1L, 2L).getUniqueIdentifier());
        Assert.assertEquals("[-2147483646,2147483645]",
                new IntegerInterval(Integer.MIN_VALUE + 2, Integer.MAX_VALUE - 2).getUniqueIdentifier());
        Assert.assertEquals("[-2147483646,2147483645]",
                new DoubleInterval(-2147483646.0, 2147483645.0, false, false).getUniqueIdentifier());
        Assert.assertEquals("[-9223372036854775806,9223372036854775805]",
                new LongInterval(Long.MIN_VALUE + 2L, Long.MAX_VALUE - 2L).getUniqueIdentifier());
        Assert.assertEquals("[-18014398509481982,18014398509481982]",
                new DoubleInterval(-18014398509481982.0, 18014398509481982.0, false, false).getUniqueIdentifier());
        Assert.assertEquals("[-180481982.0123,18014398509481982]",
                new NumberInterval<>(Double.class, -180481982.0123, 18014398509481982.0, false, false)
                        .getUniqueIdentifier());
    }
}
