package com.huatu.ztk.arena.service;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.Player;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dubbo.ArenaDubboService;
import com.huatu.ztk.arena.dubbo.ArenaPlayerDubboService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by shaojieyue
 * Created time 2016-10-09 13:37
 */
public class ArenaDubboServiceImpl implements ArenaDubboService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaDubboServiceImpl.class);

    @Autowired
    private ArenaRoomDao arenaRoomDao;

    @Autowired
    private ArenaPlayerDubboService arenaPlayerDubboService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 根据id查询房间
     *q
     * @param id
     * @return
     */
    @Override
    public ArenaRoom findById(long id) {
        final ArenaRoom arenaRoom = arenaRoomDao.findById(id);
        if (arenaRoom == null) {
            return arenaRoom;
        }
        final String roomUsersKey = RedisArenaKeys.getRoomUsersKey(id);
        List<Long> playerIds = null;
        if (CollectionUtils.isNotEmpty(arenaRoom.getPlayerIds())) {//
            playerIds = arenaRoom.getPlayerIds();
        }else {
            //竞技场还没有玩家,则从redis里面查询
            //在玩家在等待其他用户时,玩家id没有存入竞技场,只是存入了redis
            playerIds = redisTemplate.opsForSet().members(roomUsersKey).stream().map(userId -> Long.valueOf(userId)).collect(Collectors.toList());
        }

        arenaRoom.setPlayers(arenaPlayerDubboService.findBatch(playerIds));
        return arenaRoom;
    }

    /**
     * 根据id 更新指定房间的数据
     *
     * @param id
     * @param update
     */
    @Override
    public void updateById(long id, Update update) {
        arenaRoomDao.updateById(id,update);
        logger.info("arenaId={} update info,data={}",id,update);
    }
}
