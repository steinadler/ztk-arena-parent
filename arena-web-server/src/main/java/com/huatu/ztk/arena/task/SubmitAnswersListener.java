package com.huatu.ztk.arena.task;

import com.google.common.collect.Maps;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.common.Actions;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.JsonUtil;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import com.huatu.ztk.paper.bean.UserAnswers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 提交答案处理,用来给用户推送其他玩家的做题状态
 * Created by shaojieyue
 * Created time 2016-10-14 11:15
 */
public class SubmitAnswersListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(SubmitAnswersListener.class);

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Override
    public void onMessage(Message message) {
        String content = new String(message.getBody());
        logger.info("receive message,data={}", content);
        final UserAnswers userAnswers;
        try {
            userAnswers = JsonUtil.toObject(content, UserAnswers.class);
        } catch (Exception e) {
            logger.error("proccess fail. message={}", message, e);
            return;
        }

        final long uid = userAnswers.getUid();
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        final String arenaId = redisTemplate.opsForValue().get(userRoomKey);
        if (StringUtils.isBlank(arenaId)) {//该用户没有参加竞技场
            return;
        }

        final ArenaRoom arenaRoom = arenaRoomService.findById(Long.valueOf(arenaId));
        if (arenaRoom == null || arenaRoom.getStatus() == ArenaRoomStatus.FINISHED) {//房间不存在或者已经结束,则不进行处理
            logger.error("arena room status ex,arenaId={},data={}",arenaId,JsonUtil.toJson(arenaRoom));
            return;
        }

        //检查本次练习是否存在于该房间
        final int index = arenaRoom.getPractices().indexOf(userAnswers.getPracticeId());
        if (index < 0) {//不存在该房间不处理
            return;
        }

        final AnswerCard answerCard = practiceCardDubboService.findById(userAnswers.getPracticeId());
        final int rcount = answerCard.getRcount();
        Map data = Maps.newHashMap();
        data.put("arenaId",arenaId);
        data.put("uid",uid);
        data.put("rcount",rcount);
        //总的做题量
        data.put("acount",answerCard.getRcount()+answerCard.getWcount());
        data.put("action", Actions.SYSTEM_PRACTICE_STATUS_UPDATE);
        rabbitTemplate.convertAndSend("game_notify_exchange","",data);
    }
}
