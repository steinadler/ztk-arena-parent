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
        List<ArenaResult> results = Lists.newArrayList();
        final ArenaResult arenaResult = ArenaResult.builder()
                .elapsedTime(1000)
                .rcount(10)
                .uid(RandomUtils.nextInt(10000000,30000000))
                .build();
        results.add(arenaResult);

        final ArenaResult arenaResult2 = ArenaResult.builder()
                .elapsedTime(1000)
                .rcount(20)
                .uid(RandomUtils.nextInt(10000000,30000000))
                .build();
        results.add(arenaResult2);

        final ArenaResult arenaResult3 = ArenaResult.builder()
                .elapsedTime(3000)
                .rcount(20)
                .uid(RandomUtils.nextInt(10000000,30000000))
                .build();
        results.add(arenaResult3);

        final ArenaResult arenaResult4 = ArenaResult.builder()
                .elapsedTime(4000)
                .rcount(4)
                .uid(RandomUtils.nextInt(10000000,30000000))
                .build();
        results.add(arenaResult4);

        Collections.sort(results, new Comparator<ArenaResult>() {
            @Override
            public int compare(ArenaResult o1, ArenaResult o2) {
                //默认正序排
                long sub = o2.getRcount() - o1.getRcount();
                if (sub != 0) {
                    return (int) sub;
                }

                long subTime = o1.getElapsedTime() - o2.getElapsedTime();
                return (int) subTime;
            }
        });
        System.out.println(JsonUtil.toJson(results));
    }
}
