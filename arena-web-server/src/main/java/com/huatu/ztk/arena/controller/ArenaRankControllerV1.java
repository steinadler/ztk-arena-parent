package com.huatu.ztk.arena.controller;

import com.huatu.ztk.arena.bean.UserArenaRecord;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.exception.BizException;
import com.huatu.ztk.user.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 竞技排行
 * Created by shaojieyue
 * Created time 2016-10-11 16:22
 */

@RestController
@RequestMapping(value = "/v1/arenas/ranks", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ArenaRankControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(ArenaRankControllerV1.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private UserSessionService userSessionService;

    /**
     * 查询排行榜接口
     *
     * @param token
     * @return
     */
    @RequestMapping(value = "/today", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object ranks(@RequestHeader(required = false) String token) throws BizException {
        userSessionService.assertSession(token);
        long date = System.currentTimeMillis();
        //用户id
        long uid = userSessionService.getUid(token);
        //查询排行列表
        List<UserArenaRecord> list = arenaRoomService.findTodayRank(date);
        final UserArenaRecord myTodayRank = arenaRoomService.findMyTodayRank(uid, date);
        Map data = new HashMap();
        data.put("ranks", list);
        data.put("myRank", myTodayRank);
        return data;
    }
}
