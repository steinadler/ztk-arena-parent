package com.huatu.ztk.arena.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

/**
 * 关闭竞技场房间通知
 * Created by shaojieyue
 * Created time 2016-10-17 16:11
 */
public class CloseArenaListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(CloseArenaListener.class);

    @Override
    public void onMessage(Message message) {
        // TODO: 10/17/16
    }
}
