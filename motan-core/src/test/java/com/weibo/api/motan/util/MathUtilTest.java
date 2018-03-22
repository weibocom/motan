package com.weibo.api.motan.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class MathUtilTest {

    @Test
    public void parseInt() {

        assertTrue(MathUtil.parseInt("-1",0)==-1);
        assertTrue(MathUtil.parseInt("0",-1)==0);
        assertTrue(MathUtil.parseInt("1",0)==1);

        assertTrue(MathUtil.parseInt("",0)==0);
        assertTrue(MathUtil.parseInt(null,0)==0);
        assertTrue(MathUtil.parseInt("Invalid Int String",0)==0);

    }

    @Test
    public void parseLong() {

        assertTrue(MathUtil.parseLong("-1",0)==-1);
        assertTrue(MathUtil.parseLong("0",-1)==0);
        assertTrue(MathUtil.parseLong("1",0)==1);

        assertTrue(MathUtil.parseLong("",0)==0);
        assertTrue(MathUtil.parseLong(null,0)==0);
        assertTrue(MathUtil.parseLong("Invalid Int String",0)==0);
    }

    @Test
    public void getNonNegative() {

        assertTrue(MathUtil.getNonNegative(Integer.MIN_VALUE)==0);
        assertTrue(MathUtil.getNonNegative(-1)>0);

        assertTrue(MathUtil.getNonNegative(0)==0);
        assertTrue(MathUtil.getNonNegative(1)==1);
        assertTrue(MathUtil.getNonNegative(Integer.MAX_VALUE)==Integer.MAX_VALUE);

    }
}