package com.huatu.ztk.arena.controller;

import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomSimple;
import com.huatu.ztk.arena.bean.ArenaConfig;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.PageBean;
import com.huatu.ztk.commons.exception.BizException;
import com.huatu.ztk.user.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 竞技场控制层
 * Created by shaojieyue
 * Created time 2016-07-05 10:19
 */

@RestController
@RequestMapping(value = "/v1/arenas", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ArenaControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(ArenaControllerV1.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private UserSessionService userSessionService;

    /**
     * 查询我的竞技记录
     *
     * @param token
     * @param cursor
     * @return
     */
    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public Object history(@RequestHeader(required = false) String token,
                          @RequestParam(defaultValue = Long.MAX_VALUE + "") long cursor) throws BizException {
        userSessionService.assertSession(token);
        if (cursor < 1) {//说明查询第一页，那么，cursor设置为最大值
            cursor = Long.MAX_VALUE;
        }
        //用户id
        long uid = userSessionService.getUid(token);
        PageBean<ArenaRoomSimple> pageBean = arenaRoomService.history(uid, cursor, 20);
        return pageBean;
    }

    /**
     * 查询竞技记录详情
     *
     * @param token
     * @param roomId 房间id
     * @return
     */
    @RequestMapping(value = "/{roomId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ArenaRoom detail(@RequestHeader(required = false) String token, @PathVariable long roomId) throws BizException {
        userSessionService.assertSession(token);
        final long uid = userSessionService.getUid(token);
        final ArenaRoom arenaRoom = arenaRoomService.findById(roomId, uid);
        return arenaRoom;
    }

    /**
     * 竞技场基础配置
     *
     * @return
     */
    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ArenaConfig config() {
        return ArenaConfig.getConfig();
    }

}
