package com.huatu.ztk.arena.service;

import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.paper.common.PaperRewardRedisKeys;
import com.huatu.ztk.user.bean.RewardMessage;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created by linkang on 2017/10/13 上午10:56
 */

@Service
public class ArenaRewardService {

    private static final Logger logger = LoggerFactory.getLogger(ArenaRewardService.class);

    /**
     * 积分处理的队列名称
     */
    public static final String MQ_NAME = "reward_action_queue";

    /**
     * redis hash value
     */
    public static final String VALUE_MARK = "mark";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserDubboService userDubboService;

    public static final String ACTION_ARENA_WIN = "ARENA_WIN";


    /**
     * 竞技场胜利消息
     *
     * @param userId
     */
    public void sendArenaWinMsg(long userId) {
        String key = PaperRewardRedisKeys.getDayHashKey(userId);
        String hashKey = "arena_win";
        UserDto userDto = userDubboService.findById(userId);

        if (userDto == null) {
            return;
        }

        RewardMessage msg = RewardMessage.builder()
                .action(ACTION_ARENA_WIN)
                .bizId(userId + "_" + hashKey)
                .uid(userId)
                .uname(userDto.getName())
                .build();
        sendMsg(key, hashKey, 1, TimeUnit.DAYS, msg);
    }


    private void sendMsg(String key, String hashKey, final long timeout, final TimeUnit unit, RewardMessage msg) {
        HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();

        if (opsForHash.get(key, hashKey) != null) {
            return;
        }

        if (redisTemplate.hasKey(key)) {
            opsForHash.put(key, hashKey, VALUE_MARK);
            redisTemplate.expire(key, timeout, unit);
        } else {
            opsForHash.put(key, hashKey, VALUE_MARK);
        }
        rabbitTemplate.convertAndSend("", MQ_NAME, msg);

        logger.info("send msg={}", JsonUtil.toJson(msg));
    }

}
