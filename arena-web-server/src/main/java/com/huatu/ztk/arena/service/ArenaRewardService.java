package com.huatu.ztk.arena.service;

import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.user.bean.RewardMessage;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.common.UserRedisKeys;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.huatu.ztk.commons.RewardConstants.*;

/**
 * Created by linkang on 2017/10/13 上午10:56
 */

@Service
public class ArenaRewardService {

    private static final Logger logger = LoggerFactory.getLogger(ArenaRewardService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserDubboService userDubboService;

    private static final int SEND_LIMIT = 1;

    /**
     * 竞技场胜利消息
     *
     * @param userId
     */
    public void sendArenaWinMsg(long userId, long roomId) {
        String key = UserRedisKeys.getDayRewardKey(userId);
        UserDto userDto = userDubboService.findById(userId);

        if (userDto == null) {
            return;
        }

        RewardMessage msg = RewardMessage.builder()
                .action(ACTION_ARENA_WIN)
                .bizId(userId + "_" + roomId)
                .uid(userId)
                .uname(userDto.getName())
                .timestamp(System.currentTimeMillis())
                .build();
        sendMsg(key, ACTION_ARENA_WIN, 1, TimeUnit.DAYS, msg);
    }


    private void sendMsg(String key, String hashKey, final long timeout, final TimeUnit unit, RewardMessage msg) {
        HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();

        if (String.valueOf(SEND_LIMIT).equals(opsForHash.get(key, hashKey))) {
            return;
        }

        boolean exists = redisTemplate.hasKey(key);
        opsForHash.increment(key, hashKey, 1);

        if (!exists) {
            redisTemplate.expire(key, timeout, unit);
        }

        rabbitTemplate.convertAndSend("", MQ_NAME, msg);

        logger.info("send msg={}", JsonUtil.toJson(msg));
    }

}
