package com.huatu.ztk.arena.dao;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * Created by shaojieyue
 * Created time 2016-10-17 16:29
 */
@Repository
public class ArenaUserSummaryDao {
    private static final Logger logger = LoggerFactory.getLogger(ArenaUserSummaryDao.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    public void updateSummary(){
    }

    public ArenaUserSummary findById(String id) {
        return mongoTemplate.findById(id, ArenaUserSummary.class);
    }
}
