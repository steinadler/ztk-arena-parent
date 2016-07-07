package com.huatu.ztk.arena.dao;

import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.UserArenaRecord;
import com.huatu.ztk.commons.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的竞技记录 dao
 * Created by shaojieyue
 * Created time 2016-07-06 16:48
 */

@Repository
public class UserArenaRecordDao {
    private static final Logger logger = LoggerFactory.getLogger(UserArenaRecordDao.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    public UserArenaRecord findByUid(long uid){
        final UserArenaRecord userArenaRecord = mongoTemplate.findById(uid, UserArenaRecord.class);
        return userArenaRecord;
    }

    public void save(UserArenaRecord userArenaRecord) {
        logger.info("save UserArenaRecord ,data={}", JsonUtil.toJson(userArenaRecord));
        mongoTemplate.save(userArenaRecord);
    }

    /**
     * 通过id批量查询竞技记录
     * @param uidList
     * @return
     */
    public List<UserArenaRecord> findByUids(List<Long> uidList) {
        if (CollectionUtils.isEmpty(uidList)) {
            return new ArrayList<>();
        }

        Criteria[] criterias = new Criteria[uidList.size()];
        for (int i = 0; i < uidList.size(); i++) {
            criterias[i] = Criteria.where("_id").is(uidList.get(i));
        }

        Criteria criteria = new Criteria();
        criteria.orOperator(criterias);
        final List<UserArenaRecord> rooms = mongoTemplate.find(new Query(criteria), UserArenaRecord.class);
        return rooms;
    }
}
