package com.huatu.ztk.arena.task;

import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.arena.service.ArenaUserSummaryService;
import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * 关闭竞技场房间通知
 * Created by shaojieyue
 * Created time 2016-10-17 16:11
 */
public class CloseArenaListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(CloseArenaListener.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private ArenaUserSummaryService arenaUserSummaryService;

    @Autowired
    private UserDubboService userDubboService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void onMessage(Message message) {
        String text = new String(message.getBody());
        logger.info("receive message={}",text);
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

        ArenaRoom room = arenaRoomService.findById(arenaId);
        for (long uid : room.getPlayerIds()) {
            final UserDto userDto = userDubboService.findById(uid);
            if (userDto == null) {
                logger.info("uid={} not find ",uid);
                continue;
            }
            if (userDto.isRobot()) {//如果是机器人,则把其添加入等待池中,用于后续的使用
                final String robotsKey = RedisArenaKeys.getRobotsKey();
                redisTemplate.opsForSet().add(robotsKey,uid+"");
                logger.info("push uid={} to robot pool.",uid);
            }
            boolean isWinner = room.getWinner() == uid;
            arenaUserSummaryService.updateUserSummary(uid, isWinner);
        }

    }
}
