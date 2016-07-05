package com.huatu.ztk.arena.service;

import com.huatu.ztk.arena.bean.ArenaRoomSummary;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 *
 * 竞技场服务层
 * Created by shaojieyue
 * Created time 2016-07-05 10:25
 */

@Service
public class ArenaRoomService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomService.class);

    @Resource(name = "arenaRedisTemplate")
    private RedisTemplate arenaRedisTemplate;

    /**
     * 查询竞技场汇总信息
     * @return
     */
    public ArenaRoomSummary summary() {
        final List list = arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                final ListOperations listOperations = operations.opsForList();
                final ValueOperations valueOperations = operations.opsForValue();
                valueOperations.get(RedisArenaKeys.ARENA_ONLINE_COUNT);//在线人数
                listOperations.size(RedisArenaKeys.ONLINE_ROOM_LIST);//房间总数量
                listOperations.size(RedisArenaKeys.ONGOING_ROOM_LIST);//正在考试的房间
                return null;
            }
        });

        final Integer palyerCount = Integer.valueOf((String) list.get(0));
        long allRoomCount = (Long)list.get(1);//房间总数
        long ongoingRoomCount = (Long)list.get(2);//进行中数量
        long freeRoomCount = allRoomCount - ongoingRoomCount;//空闲房间数量
        final ArenaRoomSummary arenaRoomSummary = ArenaRoomSummary.builder()
                .freeCount(freeRoomCount)
                .goingCount(ongoingRoomCount)
                .playerCount(palyerCount)
                .roomCount(allRoomCount)
                .build();
        return arenaRoomSummary;
    }
}
