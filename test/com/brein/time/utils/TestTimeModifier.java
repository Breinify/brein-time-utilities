package com.brein.time.utils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestTimeModifier {

    @Test
    public void testWeeklyCutoff(){
        Assert.assertEquals(1627776000, TimeModifier.START_OF_DAY.applyModifier(1627776000));
        Assert.assertEquals(1627776000, TimeModifier.START_OF_WEEK.applyModifier(1627948289));
        Assert.assertEquals(1627776000, TimeModifier.START_OF_WEEK.applyModifier(1627776000));
    }

}