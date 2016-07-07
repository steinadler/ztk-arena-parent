package com.huatu.ztk.arena.task;

import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shaojieyue
 * Created time 2016-05-30 20:44
 */
public class PracticeMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(PracticeMessageListener.class);


    @Autowired
    private ArenaRoomService arenaRoomService;

    public void onMessage(Message message) {
        String text = new String(message.getBody());
        logger.info("receive message={}",text);
        Map data = new HashMap();
        try {
            data = JsonUtil.toMap(text);
        }catch (Exception e){
            logger.error("ex",e);
        }
        Long id = null;
        if (data.containsKey("id")) {
            id = Longs.tryParse(data.get("id").toString());
        }
        if (id == null) {
            logger.error("message not contain key id,skip it. data={}",text);
            return;
        }

        arenaRoomService.addArenaResult(id);
    }
}
