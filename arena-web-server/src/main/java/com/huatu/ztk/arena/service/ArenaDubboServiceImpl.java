package com.huatu.ztk.arena.service;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.Player;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dubbo.ArenaDubboService;
import com.huatu.ztk.arena.dubbo.ArenaPlayerDubboService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;

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

    /**
     * 根据id查询房间
     *
     * @param id
     * @return
     */
    @Override
    public ArenaRoom findById(long id) {
        final ArenaRoom arenaRoom = arenaRoomDao.findById(id);
        if (CollectionUtils.isNotEmpty(arenaRoom.getPlayerIds())) {
            final List<Player> players = arenaRoom.getPlayerIds().stream().map(uid -> {
                return arenaPlayerDubboService.findById(uid);
            }).collect(Collectors.toList());
            arenaRoom.setPlayers(players);
        }
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
        logger.info("roomId={} update info,data={}",id,update);
    }
}
