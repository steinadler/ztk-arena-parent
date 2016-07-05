package com.huatu.ztk.arena;

import com.huatu.ztk.arena.bean.ArenaRoomSummary;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.service.ArenaRoomService;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;

import javax.annotation.Resource;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Created by shaojieyue
 * Created time 2016-07-05 12:46
 */
public class ArenaRoomServiceTest extends BaseTest{
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomServiceTest.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Resource(name = "arenaRedisTemplate")
    private RedisTemplate arenaRedisTemplate;

    @Test
    public void summaryTest(){
//        final ValueOperations valueOperations = arenaRedisTemplate.opsForValue();
//        final int count = Integer.valueOf(valueOperations.get(RedisArenaKeys.ARENA_ONLINE_COUNT).toString());
//        final int ongoingRoomCount = RandomUtils.nextInt(1,roomCount);
//        arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
//            public Object execute(RedisOperations operations) throws DataAccessException {
//                operations.opsForValue().set(RedisArenaKeys.ARENA_ONLINE_COUNT,count+"");
//                final ListOperations listOperations = operations.opsForList();
//
//                for (int i = 0; i < roomCount; i++) {
//                    listOperations.leftPush(RedisArenaKeys.ONLINE_ROOM_LIST,i+"");
//                }
//
//                for (int i = 0; i < ongoingRoomCount; i++) {
//                    listOperations.leftPush(RedisArenaKeys.ONGOING_ROOM_LIST,i+"");
//                }
//
//                return null;
//            }
//        });
//
//        final ArenaRoomSummary summary = arenaRoomService.summary();
//        Assert.assertEquals(summary.getGoingCount(),ongoingRoomCount);
//        Assert.assertEquals(summary.getPlayerCount(),count);
//        Assert.assertEquals(summary.getRoomCount(),roomCount);
//        Assert.assertEquals(summary.getFreeCount(),roomCount-ongoingRoomCount);
    }
}
