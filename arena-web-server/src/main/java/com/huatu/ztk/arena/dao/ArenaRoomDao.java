package com.huatu.ztk.arena.dao;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomSimple;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.paper.common.AnswerCardStatus;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shaojieyue
 * Created time 2016-07-05 15:21
 */

@Repository
public class ArenaRoomDao {
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomDao.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 查询竞技记录详情
     *
     * @param arenaId 竞技房间id
     * @return
     */
    public ArenaRoom findById(long arenaId) {
        return mongoTemplate.findById(arenaId, ArenaRoom.class);
    }

    /**
     * 保存竞技信息
     *
     * @param arenaRoom
     */
    public void save(ArenaRoom arenaRoom) {
        mongoTemplate.save(arenaRoom);
    }

    /**
     * 创建一个竞技房间
     *
     * @return
     */
    public void insert(ArenaRoom arenaRoom) {
        logger.info("insert arena room ,data={}", JsonUtil.toJson(arenaRoom));
        mongoTemplate.insert(arenaRoom);
    }

    /**
     * 根据arenaId 更新指定房间的数据
     *
     * @param arenaId
     * @param update
     */
    public void updateById(long arenaId, Update update) {
        if (update == null) {
            return;
        }
        final Query query = new Query(Criteria.where("_id").is(arenaId));
        mongoTemplate.updateFirst(query, update, "ztk_arena_room");
    }

    /**
     * 通过id批量查询房间信息
     *
     * @param roomIds
     * @return
     */
    public List<ArenaRoom> findByIds(List<Long> roomIds) {
        if (CollectionUtils.isEmpty(roomIds)) {
            return new ArrayList<>();
        }

        Criteria[] criterias = new Criteria[roomIds.size()];
        for (int i = 0; i < roomIds.size(); i++) {
            criterias[i] = Criteria.where("_id").is(roomIds.get(i));
        }

        Criteria criteria = new Criteria();
        criteria.orOperator(criterias);
        final List<ArenaRoom> rooms = mongoTemplate.find(new Query(criteria), ArenaRoom.class);
        return rooms;
    }

    /**
     * 根据练习id查询竞技场
     *
     * @param practiceId
     * @return
     */
    public ArenaRoom findByPracticeId(long practiceId) {
        final Criteria criteria = Criteria.where("practices").in(practiceId);
        final List<ArenaRoom> rooms = mongoTemplate.find(new Query(criteria), ArenaRoom.class);
        ArenaRoom arenaRoom = null;
        if (CollectionUtils.isNotEmpty(rooms)) {
            arenaRoom = rooms.get(0);
        }

        return arenaRoom;
    }

    /**
     * 分页查询我的竞技记录
     *
     * @param uid    用户id
     * @param cursor 游标
     * @return
     */
    public List<ArenaRoomSimple> findForPage(long uid, long cursor) {

        //返回playerIds包含uid，竞技比赛已结束的结果集
        final Criteria criteria = Criteria.where("playerIds").in(uid)
                .and("status").is(ArenaRoomStatus.FINISHED)
                .and("_id").lt(cursor);

        //设置数据返回记录条数，按创建时间倒序排列
        Query query = new Query(criteria);
        query.with(new Sort(Sort.Direction.DESC, "createTime"));

        List<ArenaRoom> records = mongoTemplate.find(query, ArenaRoom.class);
        //转换方法返回list类型
        List<ArenaRoomSimple> results = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(records)) {
            results.addAll(records);
        }
        return results;
    }
}
