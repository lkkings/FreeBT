package com.lkd.bt.spider;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Shorts;
import com.lkd.bt.spider.util.BTUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

/**
 * Created by lkkings on 2023/8/24
 */
@RunWith(ExtendedTestRunner.class)
public class UtilsTest{

    @ExtendedTest
    @Test
    public void generateNodeId() {
        String bytes = BTUtil.generateMessageID();
        System.out.println();
    }



}
