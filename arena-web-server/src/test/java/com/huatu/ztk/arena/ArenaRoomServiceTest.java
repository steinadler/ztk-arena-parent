package com.huatu.ztk.arena;

import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.common.ArenaErrors;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.spring.BizException;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;

import javax.annotation.Resource;

import static com.huatu.ztk.arena.common.ArenaErrors.ROOM_NOT_EXIST;

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
            final ArenaRoom arenaRoom = arenaRoomService.create(-1, count);
            Assert.assertNotNull(arenaRoom);
            Assert.assertEquals(arenaRoom.getQcount(),ArenaRoomService.ARENA_QCOUNT);
            Assert.assertTrue(arenaRoom.getCreateTime()>0);
            Assert.assertTrue(arenaRoom.getId()>0);
            Assert.assertEquals(arenaRoom.getMaxPlayerCount(),count);
            Assert.assertEquals(arenaRoom.getTime(),ArenaRoomService.ARENA_LIMIT_TIME);
            Assert.assertNotNull(arenaRoom.getPracticePaper());
        }
    }
}
