package com.huatu.ztk.arena;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaResult;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.commons.JsonUtil;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by shaojieyue
 * Created time 2016-07-06 16:04
 */
public class AATest {
    private static final Logger logger = LoggerFactory.getLogger(AATest.class);

    public static void main(String[] args) {
        List<Integer> results = Lists.newArrayList();
        results.add(2);
        results.add(1);
        results.add(4);
        results.add(3);

        Collections.sort(results, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2-o1;
            }
        });
        System.out.println(JsonUtil.toJson(results));
    }
}
