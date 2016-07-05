package com.huatu.ztk.arena.dao;

import com.huatu.ztk.arena.bean.ArenaRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * Created by shaojieyue
 * Created time 2016-07-05 15:21
 */

@Repository
public class ArenaRoomDao {
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomDao.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    public ArenaRoom findById(long roomId) {
        return mongoTemplate.findById(roomId,ArenaRoom.class);
    }

    public void save(ArenaRoom arenaRoom) {
        mongoTemplate.save(arenaRoom);
    }

    public void insert(ArenaRoom arenaRoom) {
        mongoTemplate.insert(arenaRoom);
    }
}
