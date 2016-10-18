package com.huatu.ztk.arena.task;

import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by shaojieyue
 * Created time 2016-10-17 17:29
 */
@Component
public class CheckAreanTask {
    private static final Logger logger = LoggerFactory.getLogger(CheckAreanTask.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final long ONE_HOUR = 60 * 60 * 1000L;

    //定时任务隔2分钟执行一次
    @Scheduled(cron = "0 0/2 * * * ?")
    public void run(){
        logger.info("auto close room task start.");

        long currentTime = System.currentTimeMillis();
        long threeHoursAgo = currentTime - 3 * ONE_HOUR;

        //查询运行中的3小时以内的房间
        Criteria criteria = Criteria.where("status").is(ArenaRoomStatus.RUNNING).and("createTime").gt(threeHoursAgo);
        final List<ArenaRoom> rooms = mongoTemplate.find(new Query(criteria), ArenaRoom.class);

        for (ArenaRoom room : rooms) {
            long arenaEndTime = room.getCreateTime() + room.getLimitTime();

            //超过做题时间,将房间关闭
            if (currentTime > arenaEndTime) {
                logger.info("auto close room ,obj={}", JsonUtil.toJson(room));
                try {
                    arenaRoomService.closeArena(room.getId());
                } catch (Exception e) {
                    logger.error("auto close fail.", e);
                }
            }
        }

    }
}
