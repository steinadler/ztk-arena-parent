package com.huatu.ztk.arena.task;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaConfig;
import com.huatu.ztk.arena.bean.ArenaResult;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.commons.exception.BizException;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by shaojieyue
 * Created time 2016-10-17 17:29
 */
@Component
public class CheckAreanTask {
    private static final Logger logger = LoggerFactory.getLogger(CheckAreanTask.class);


    @Autowired
    private MongoTemplate mongoTemplate;


    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Autowired
    private DistributedLock distributedLock;

    private static final long ONE_HOUR = 60 * 60 * 1000L;

    /**
     * 延迟关闭时间长度
     */
    public static final int DELAY_CLOSE_TIME = 3*60*1000;


    @PostConstruct
    public void init() {
        //添加停止任务线程
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                //释放定时任务锁
                redisTemplate.delete(RedisArenaKeys.getScheduledLockKey());
                logger.info("release lock,lock={}",RedisArenaKeys.getScheduledLockKey());
            }
        }));
    }

    //定时任务隔2分钟执行一次
    @Scheduled(cron = "0 0/2 * * * ?")
    public void run(){
        logger.info("auto close room task start.");
        final String lockKey = RedisArenaKeys.getScheduledLockKey();
        long maxHoldTime = 4*60*1000;
        final boolean success = distributedLock.getLock(lockKey,maxHoldTime);
        if (!success) {//没有获取到锁
            return;
        }
        logger.info("get the lock and start auto close room.");
        long currentTime = System.currentTimeMillis();
        long threeHoursAgo = currentTime - 3 * ONE_HOUR;

        //查询运行中的3小时以内的房间
        Criteria criteria = Criteria.where("status").is(ArenaRoomStatus.RUNNING).and("createTime").gt(threeHoursAgo);
        final List<ArenaRoom> rooms = mongoTemplate.find(new Query(criteria), ArenaRoom.class);

        for (ArenaRoom room : rooms) {
            //关闭时间 要加上延迟时间,防止用户时间进行完之后提交,到时提交数据无效
            long arenaEndTime = room.getCreateTime() + room.getLimitTime()*1000 + DELAY_CLOSE_TIME;
            //超过做题时间,将房间关闭
            if (currentTime > arenaEndTime) {
                logger.info("auto close room ,obj={}", JsonUtil.toJson(room));

                //这里通过帮助用户提交试卷,来达到关闭竞技场的目的
                final List<Long> practices = room.getPractices();
                final List<Long> playerIds = room.getPlayerIds();
                if (CollectionUtils.isEmpty(practices)) {
                    //没有练习的竞技场,则不进行处理
                    continue;
                }
                for (int i = 0; i < playerIds.size(); i++) {
                    final Long playerId = playerIds.get(i);

                    //说明用户没有交卷
                    if (Arrays.stream(room.getResults()).noneMatch(result -> playerId == result.getUid())) {
                        try {
                            //帮用户提交试卷
                            practiceCardDubboService.submitAnswers(practices.get(i), playerId, Lists.newArrayList(),true,-9);
                        } catch (BizException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        //释放锁
        distributedLock.tryReleaseLock(lockKey,maxHoldTime);

    }

}
