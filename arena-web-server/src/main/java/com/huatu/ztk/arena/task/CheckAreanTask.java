package com.huatu.ztk.arena.task;

import com.huatu.ztk.arena.service.ArenaRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by shaojieyue
 * Created time 2016-10-17 17:29
 */
public class CheckAreanTask {
    private static final Logger logger = LoggerFactory.getLogger(CheckAreanTask.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    public void run(){
        // TODO: 10/17/16 检查超过做题时间,没有关闭的房间
        //检查运行中的
        //3小时以内
        arenaRoomService.closeArena(arenaId);
    }
}
