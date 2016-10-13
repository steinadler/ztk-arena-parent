package com.huatu.ztk.arena;

import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.service.ArenaRoomService;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

/**
 * Created by shaojieyue
 * Created time 2016-07-05 12:46
 */
public class ArenaRoomServiceTest extends BaseTest{
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomServiceTest.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Resource(name = "redisTemplate")
    private RedisTemplate redisTemplate;

    long uid = 12252065;

    @Test
    public void createTest(){
        int[] counts = new int[]{2,4,8};
        for (int i = 0; i < 60; i++) {
            int count = counts[RandomUtils.nextInt(0,counts.length)];
            final ArenaRoom arenaRoom = arenaRoomService.create(-1);
            Assert.assertNotNull(arenaRoom);
            Assert.assertEquals(arenaRoom.getQcount(), ArenaConfig.getConfig().getQuestionCount());
            Assert.assertTrue(arenaRoom.getCreateTime()>0);
            Assert.assertTrue(arenaRoom.getId()>0);
            Assert.assertNotNull(arenaRoom.getPracticePaper());
        }
    }
}
