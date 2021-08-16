package com.brein.time.utils;

import org.junit.Assert;
import org.junit.Test;

public class TestTimeModifier {

    @Test
    public void testWeeklyCutoff() {
        Assert.assertEquals(1627776000, TimeModifier.START_OF_DAY.applyModifier(1627776000));
        Assert.assertEquals(1627776000, TimeModifier.START_OF_WEEK.applyModifier(1627948289));
        Assert.assertEquals(1627776000, TimeModifier.START_OF_WEEK.applyModifier(1627776000));
    }

    @Test
    public void tesMoveTimeByUnit() {
        Assert.assertEquals(1629194400, TimeModifier.START_OF_DAY.moveTimeByUnit(1629108000, false, 1));
        Assert.assertEquals(1629280800, TimeModifier.START_OF_DAY.moveTimeByUnit(1629108000, false, 2));
        Assert.assertEquals(1629021600, TimeModifier.START_OF_DAY.moveTimeByUnit(1629108000, false, -1));
        Assert.assertEquals(1629158400, TimeModifier.START_OF_DAY.moveTimeByUnit(1629108000, true, 1));
        Assert.assertEquals(1629244800, TimeModifier.START_OF_DAY.moveTimeByUnit(1629108000, true, 2));

        Assert.assertEquals(1627776000, TimeModifier.START_OF_MONTH.moveTimeByUnit(1629108000, true, 0));
        Assert.assertEquals(1629244800, TimeModifier.START_OF_MONTH.moveTimeByUnit(1629108000, true, 1));
    }
}