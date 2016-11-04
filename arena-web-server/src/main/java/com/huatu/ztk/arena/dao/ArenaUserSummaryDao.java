package com.huatu.ztk.arena.dao;

import com.huatu.ztk.arena.bean.ArenaUserSummary;
import com.huatu.ztk.commons.JsonUtil;
import com.mongodb.WriteResult;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 竞技场统计dao
 * Created by shaojieyue
 * Created time 2016-10-17 16:29
 */
@Repository
public class ArenaUserSummaryDao {
    private static final Logger logger = LoggerFactory.getLogger(ArenaUserSummaryDao.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据id更新竞技场统计信息
     * @param id
     * @param update
     */
    public boolean updateSummary(String id,Update update){
        final Query query = Query.query(Criteria.where("_id").is(id));
        WriteResult writeResult = mongoTemplate.updateFirst(query, update, ArenaUserSummary.class);
        return writeResult.isUpdateOfExisting();
    }

    /**
     * 根据uid查询该用户的竞技统计
     *
     * @return
     */
    public ArenaUserSummary findById(String id) {
        return mongoTemplate.findById(id, ArenaUserSummary.class);
    }

    /**
     * 获取用户当天统计id
     * @param uid
     * @return
     */
    public String getTodaySummaryId(long uid){
        return uid+ DateFormatUtils.format(System.currentTimeMillis(),"yyyyMMdd");
    }

    /**
     * 获取用户总统计id
     * @param uid
     * @return
     */
    public String getTotalSummaryId(long uid){
        return uid+"-1";
    }


    /**
     * 插入一条统计记录
     * @param arenaUserSummary
     */
    public void insertSummary(ArenaUserSummary arenaUserSummary) {
        logger.info("insert new summary={}", JsonUtil.toJson(arenaUserSummary));
        mongoTemplate.insert(arenaUserSummary);
    }
}
