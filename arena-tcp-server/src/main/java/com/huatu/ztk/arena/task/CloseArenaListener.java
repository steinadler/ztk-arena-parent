package com.huatu.ztk.arena.task;

import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.common.UserChannelCache;
import com.huatu.ztk.arena.netty.SuccessReponse;
import com.huatu.ztk.commons.JsonUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shaojieyue
 * Created time 2016-10-18 17:45
 */
public class CloseArenaListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(CloseArenaListener.class);

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private UserChannelCache userChannelCache;

    @Override
    public void onMessage(Message message) {
        String text = new String(message.getBody());
        logger.info("receive close arena message={}",text);
        Map data = new HashMap();
        try {
            data = JsonUtil.toMap(text);
        }catch (Exception e){
            logger.error("ex",e);
        }
        Long arenaId = null;
        if (data.containsKey("arenaId")) {
            arenaId = Longs.tryParse(data.get("arenaId").toString());
        }

        if (arenaId == null) {
            logger.error("message not contain key arenaId,skip it. data={}",text);
            return;
        }
        proccessArenaView(arenaId);
    }

    /**
     * 查看竞技结果通知
     * @param arenaId
     */
    private void proccessArenaView(long arenaId) {
        final long[] users = getRoomUsers(arenaId);
        for (long user : users) {//遍历,挨个发送通知
            final Channel channel = userChannelCache.getChannel(user);
            if (channel != null) {
                //通知用户查看结果
                channel.writeAndFlush(SuccessReponse.arenaView(arenaId));
            }
        }
    }

    /**
     * 查询竞技房间里的人员
     * @param arenaId
     * @return
     */
    private long[] getRoomUsers(long arenaId) {
        final SetOperations<String, String> setOperations = redisTemplate.opsForSet();
        final String roomUsersKey = RedisArenaKeys.getRoomUsersKey(arenaId);
        return setOperations.members(roomUsersKey).stream().mapToLong(uid-> Long.valueOf(uid)).toArray();
    }
}
