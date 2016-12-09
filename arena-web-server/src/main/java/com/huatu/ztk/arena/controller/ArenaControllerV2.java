package com.huatu.ztk.arena.controller;

import com.google.common.collect.Lists;
import com.huatu.ztk.arena.bean.ArenaConfig;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomSimple;
import com.huatu.ztk.arena.bean.ArenaUserSummary;
import com.huatu.ztk.arena.dubbo.ArenaUserSummaryDubboService;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.PageBean;
import com.huatu.ztk.commons.exception.BizException;
import com.huatu.ztk.commons.exception.CommonErrors;
import com.huatu.ztk.user.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 竞技场控制层
 * Created by shaojieyue
 * Created time 2016-07-05 10:19
 */

@RestController
@RequestMapping(value = "/v2/arenas", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ArenaControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(ArenaControllerV2.class);



    /**
     * 竞技场基础配置
     *
     * @return
     */
    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map config() {
        Map data = new HashMap();
        List<Map> modules = Lists.newArrayList();
        data.put("waitTime",8);
        data.put("modules",modules);
        return data;
    }
}
