package com.huatu.ztk.arena;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaResult;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.commons.JsonUtil;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by shaojieyue
 * Created time 2016-07-06 16:04
 */
public class AATest {
    private static final Logger logger = LoggerFactory.getLogger(AATest.class);

    public static void main(String[] args) {
        int[] ints = new int[]{1,3,3,4};
        System.out.println(Arrays.stream(ints).anyMatch(i->i==1));
        System.out.println(Arrays.stream(ints).anyMatch(i->i==3));
        System.out.println(Arrays.stream(ints).anyMatch(i->i==5));

    }
}
