package com.brein.time.timeintervals.docs;

import com.brein.time.timeintervals.collections.ListIntervalCollection;
import com.brein.time.timeintervals.indexes.IntervalTree;
import com.brein.time.timeintervals.indexes.IntervalTreeBuilder;
import com.brein.time.timeintervals.indexes.IntervalTreeBuilder.IntervalType;
import com.brein.time.timeintervals.intervals.IInterval;
import com.brein.time.timeintervals.intervals.LongInterval;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class TestMainReadme {

    @Test
    public void testOverlap() {
        final IntervalTree tree = IntervalTreeBuilder.newBuilder()
                .usePredefinedType(IntervalType.LONG)
                .collectIntervals(interval -> new ListIntervalCollection())
                .build();
        tree.add(new LongInterval(1L, 5L));
        tree.add(new LongInterval(2L, 5L));
        tree.add(new LongInterval(3L, 5L));

        final Collection<IInterval> overlap = tree.overlap(new LongInterval(2L, 2L));
        // will print out [1, 5] and [2, 5]
        Assert.assertEquals(Arrays.asList(new LongInterval(1L, 5L), new LongInterval(2L, 5L)), overlap);

        final Collection<IInterval> find = tree.find(new LongInterval(2L, 5L));
        // will print out only [2, 5]
        Assert.assertEquals(Collections.singletonList(new LongInterval(2L, 5L)), find);
    }
}
