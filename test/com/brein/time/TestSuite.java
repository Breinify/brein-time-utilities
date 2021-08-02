package com.brein.time;

import com.brein.time.timeintervals.docs.TestDocsSuite;
import com.brein.time.timeintervals.indexes.TestIntervalTree;
import com.brein.time.timeintervals.intervals.TestInterval;
import com.brein.time.timeseries.TestBucketEndPoints;
import com.brein.time.timeseries.TestBucketTimeSeries;
import com.brein.time.timeseries.TestContainerBucketTimeSeries;
import com.brein.time.timeseries.gson.TestBucketTimeSeriesTypeConverter;
import com.brein.time.timeseries.gson.TestContainerBucketTimeSeriesTypeConverter;
import com.brein.time.utils.TestTimeModifier;
import com.brein.time.utils.TestTimeTruncater;
import com.brein.time.utils.TestTimeUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestTimeUtils.class,
        TestTimeTruncater.class,
        TestBucketTimeSeries.class,
        TestContainerBucketTimeSeries.class,
        TestBucketEndPoints.class,
        TestBucketTimeSeriesTypeConverter.class,
        TestContainerBucketTimeSeriesTypeConverter.class,
        TestInterval.class,
        TestIntervalTree.class,

        TestDocsSuite.class,

        TestTimeModifier.class
})
public class TestSuite {
}
