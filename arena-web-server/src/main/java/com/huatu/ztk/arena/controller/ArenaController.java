package com.huatu.ztk.arena.controller;

import com.huatu.ztk.arena.bean.ArenaRoomSummary;
import com.huatu.ztk.arena.service.ArenaRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 竞技场控制层
 * Created by shaojieyue
 * Created time 2016-07-05 10:19
 */

@RestController
@RequestMapping(value = "/v1/arenas/",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ArenaController {
    private static final Logger logger = LoggerFactory.getLogger(ArenaController.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    /**
     * 练习统计
     * @return
     */
    @RequestMapping(value = "summary")
    public Object summary(){
        ArenaRoomSummary arenaRoomSummary = arenaRoomService.summary();
        return arenaRoomSummary;
    }
}
