package com.huatu.ztk.arena.controller;

import com.google.common.base.Strings;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomSummary;
import com.huatu.ztk.arena.bean.UserArenaRecord;
import com.huatu.ztk.arena.service.ArenaRoomService;
import com.huatu.ztk.commons.PageBean;
import com.huatu.ztk.commons.spring.BizException;
import com.huatu.ztk.paper.bean.PracticeCard;
import com.huatu.ztk.user.common.UserErrors;
import com.huatu.ztk.user.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
@RequestMapping(value = "/v1/arenas",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ArenaControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(ArenaControllerV1.class);

    @Autowired
    private ArenaRoomService arenaRoomService;

    @Autowired
    private UserSessionService userSessionService;

    /**
     * 练习统计
     * @return
     */
    @RequestMapping(value = "summary",method = RequestMethod.GET)
    public Object summary(){
        ArenaRoomSummary arenaRoomSummary = arenaRoomService.summary();
        return arenaRoomSummary;
    }

    /**
     * 查询房间列表
     * @param cursor
     * @return
     */
    @RequestMapping(value = "")
    public Object list(@RequestParam long cursor,@RequestParam int type){
        cursor = Long.max(cursor,0);
        PageBean arenaRoomPage = arenaRoomService.findForPage(cursor,type);
        return arenaRoomPage;
    }

    /**
     * 根据id查询房间信息
     * @param roomId
     * @return
     */
    @RequestMapping(value = "{roomId}" ,method = RequestMethod.GET)
    public Object get(@PathVariable long roomId){
        final ArenaRoom arenaRoom = arenaRoomService.findById(roomId);
        return arenaRoom;
    }

    /**
     * 添加竞技场指定房间玩家
     * @param roomId 房间id
     * @param token
     * @return
     */
    @RequestMapping(value = "{roomId}/players" ,method = RequestMethod.PUT)
    public Object joinRoom(@PathVariable long roomId, @RequestHeader String token) throws BizException {
        if (Strings.isNullOrEmpty(token) || userSessionService.isExpire(token)) {//用户会话过期
            return UserErrors.SESSION_EXPIRE;
        }
        //用户id
        long uid = userSessionService.getUid(token);
        //加入房间
        ArenaRoom arenaRoom = arenaRoomService.joinRoom(roomId,uid);
        return arenaRoom;
    }

    /**
     * 玩家退出指定房间
     * @param roomId 房间id
     * @param token
     * @return
     */
    @RequestMapping(value = "{roomId}/players" ,method = RequestMethod.DELETE)
    public Object quitRoom(@PathVariable long roomId, @RequestHeader String token) throws BizException {
        if (Strings.isNullOrEmpty(token) || userSessionService.isExpire(token)) {//用户会话过期
            return UserErrors.SESSION_EXPIRE;
        }
        //用户id
        long uid = userSessionService.getUid(token);
        final ArenaRoom arenaRoom = arenaRoomService.quitRoom(roomId, uid);
        return arenaRoom;
    }

    /**
     * 智能加入房间
     * @param token
     * @return
     */
    @RequestMapping(value = "smartJoin",method = RequestMethod.PUT)
    public Object smartJoin( @RequestHeader String token) throws BizException {
        if (Strings.isNullOrEmpty(token) || userSessionService.isExpire(token)) {//用户会话过期
            return UserErrors.SESSION_EXPIRE;
        }
        //用户id
        long uid = userSessionService.getUid(token);
        final ArenaRoom arenaRoom = arenaRoomService.smartJoin(uid);
        return arenaRoom;
    }

    /**
     * 用户发起pk请求
     * @param roomId roomId
     * @param terminal 终端
     * @param token
     * @return
     * @throws BizException
     */
    @RequestMapping(value = "{roomId}/pk",method = RequestMethod.POST)
    public Object startPk(@PathVariable long roomId,@RequestHeader int terminal,@RequestHeader String token) throws BizException {
        if (Strings.isNullOrEmpty(token) || userSessionService.isExpire(token)) {//用户会话过期
            return UserErrors.SESSION_EXPIRE;
        }
        //用户id
        long uid = userSessionService.getUid(token);
        final PracticeCard practiceCard = arenaRoomService.startPk(roomId, uid, terminal);
        return practiceCard;
    }

    /**
     * 查询我的竞技记录
     * @param token
     * @param cursor
     * @return
     */
    @RequestMapping(value = "/myArenas",method = RequestMethod.GET)
    public Object myArenas(@RequestHeader String token,@RequestParam long cursor){
        if (Strings.isNullOrEmpty(token) || userSessionService.isExpire(token)) {//用户会话过期
            return UserErrors.SESSION_EXPIRE;
        }
        //用户id
        long uid = userSessionService.getUid(token);
        PageBean<ArenaRoom> pageBean = arenaRoomService.findMyArenas(uid,cursor);
        return pageBean;
    }

    /**
     * 查询排行榜接口
     * @param token
     * @return
     */
    @RequestMapping(value = "/ranks",method = RequestMethod.GET)
    public Object ranks(@RequestHeader String token){
        if (Strings.isNullOrEmpty(token) || userSessionService.isExpire(token)) {//用户会话过期
            return UserErrors.SESSION_EXPIRE;
        }
        //用户id
        long uid = userSessionService.getUid(token);
        //查询排行列表
        List<UserArenaRecord> list = arenaRoomService.findRank();
        //查询我的排行
        long myRank = arenaRoomService.findMyRank(uid);
        Map data = new HashMap();
        data.put("ranks",list);
        data.put("myRank",myRank);
        return data;
    }

}
