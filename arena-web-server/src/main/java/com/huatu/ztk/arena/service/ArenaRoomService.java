package com.huatu.ztk.arena.service;

import com.google.common.primitives.Ints;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.bean.ArenaRoomSummary;
import com.huatu.ztk.arena.common.ArenaErrors;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.commons.Module;
import com.huatu.ztk.commons.ModuleConstants;
import com.huatu.ztk.commons.SubjectType;
import com.huatu.ztk.commons.spring.BizException;
import com.huatu.ztk.paper.api.PracticeDubboService;
import com.huatu.ztk.paper.bean.PracticePaper;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 竞技场服务层
 * Created by shaojieyue
 * Created time 2016-07-05 10:25
 */

@Service
public class ArenaRoomService {
    private static final Logger logger = LoggerFactory.getLogger(ArenaRoomService.class);
    public static final int ARENA_QCOUNT = 20;
    /**
     * 竞技限时
     */
    public static final int ARENA_LIMIT_TIME = 60*20;

    @Resource(name = "arenaRedisTemplate")
    private RedisTemplate arenaRedisTemplate;

    @Autowired
    private ArenaRoomDao arenaRoomDao;

    @Autowired
    private PracticeDubboService practiceDubboService;

    /**
     * 查询竞技场汇总信息
     * @return
     */
    public ArenaRoomSummary summary() {
        final List list = arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                final ListOperations listOperations = operations.opsForList();
                final ValueOperations valueOperations = operations.opsForValue();
                valueOperations.get(RedisArenaKeys.ARENA_ONLINE_COUNT);//在线人数
                listOperations.size(RedisArenaKeys.ONLINE_ROOM_LIST);//房间总数量
                listOperations.size(RedisArenaKeys.ONGOING_ROOM_LIST);//正在考试的房间
                return null;
            }
        });

        final Integer palyerCount = Integer.valueOf((String) list.get(0));
        long allRoomCount = (Long)list.get(1);//房间总数
        long ongoingRoomCount = (Long)list.get(2);//进行中数量
        long freeRoomCount = allRoomCount - ongoingRoomCount;//空闲房间数量
        final ArenaRoomSummary arenaRoomSummary = ArenaRoomSummary.builder()
                .freeCount(freeRoomCount)
                .goingCount(ongoingRoomCount)
                .playerCount(palyerCount)
                .roomCount(allRoomCount)
                .build();
        return arenaRoomSummary;
    }

    /**
     * 用户加入指定房间
     * @param roomId 房间id
     * @param uid 用户id
     * @return
     */
    public ArenaRoom joinRoom(final long roomId, long uid) throws BizException {
        logger.info("user join room. roomId = {}, uid = {}",roomId, uid);
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        final ValueOperations valueOperations = arenaRedisTemplate.opsForValue();
        final String userRoom = (String)valueOperations.get(userRoomKey);
        ArenaRoom arenaRoom = arenaRoomDao.findById(roomId);

        if (arenaRoom == null) {//房间不存在
            throw new BizException(ArenaErrors.ROOM_NOT_EXIST);
        }

        //正进行的或已结束的不允许加入
        if (arenaRoom.getStatus() != ArenaRoomStatus.CREATED) {
            throw new BizException(ArenaErrors.FINISHED_ONGOING_CAN_NOT_JOIN);
        }

        //已经存在该房间内
        if (arenaRoom.getPlayers().indexOf(uid) >= 0) {
            return arenaRoom;//直接返回房间数据
        }

        if (StringUtils.isNoneBlank(userRoom)) {//房间不为空,则说明已经存在于房间
            throw new BizException(ArenaErrors.USER_IN_ROOM);
        }

        //更新redis数据
        arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                //设置用户存在的房间号
                operations.opsForValue().set(userRoomKey,roomId+"");
                //在线人数+1
                operations.opsForValue().increment(RedisArenaKeys.getArenaOnlineCount(),1);
                return null;
            }
        });


        arenaRoom.getPlayers().add(uid);//添加用户到房间
        //更新房间数据
        arenaRoomDao.save(arenaRoom);
        return arenaRoom;
    }

    /**
     * 退出房间
     * @param roomId 房间号
     * @param uid 用户id
     * @throws BizException
     */
    public ArenaRoom quitRoom(long roomId, long uid) throws BizException {
        logger.info("user quit room. roomId = {}, uid = {}",roomId, uid);
        final String userRoomKey = RedisArenaKeys.getUserRoomKey(uid);
        final ValueOperations valueOperations = arenaRedisTemplate.opsForValue();
        final String userRoom = (String)valueOperations.get(userRoomKey);

        if (StringUtils.isBlank(userRoom)) {//不存在房间内,则直接返回
            throw new BizException(ArenaErrors.USER_QUIT_ROOM_NOT_MATCH);
        }
        //用户当前所在房间
        long userCurrentRoomId = Ints.tryParse(userRoom);
        if (userCurrentRoomId != roomId) {//
            throw new BizException(ArenaErrors.USER_QUIT_ROOM_NOT_MATCH);
        }

        final ArenaRoom arenaRoom = arenaRoomDao.findById(roomId);

        if (arenaRoom == null) {//房间不存在
            throw new BizException(ArenaErrors.ROOM_NOT_EXIST);
        }

        if (arenaRoom.getStatus() != ArenaRoomStatus.CREATED) {//非创建状态的不允许退出
            throw new BizException(ArenaErrors.FINISHED_ONGOING_CAN_NOT_QUIT);
        }

        arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                //删除用户房间
                operations.delete(userRoomKey);
                //在线人数减一
                operations.opsForValue().increment(RedisArenaKeys.getArenaOnlineCount(),-1);
                return null;
            }
        });

        //删除用户记录
        arenaRoom.getPlayers().remove(uid);
        arenaRoomDao.save(arenaRoom);
        return arenaRoom;
    }

    /**
     * 随机创建一个房间
     * @return
     */
    public ArenaRoom create(int playerCount){
        final int index = RandomUtils.nextInt(0, ModuleConstants.GOWUYUAN_MODULES.size());
        final Module module = ModuleConstants.GOWUYUAN_MODULES.get(index);
        final PracticePaper practicePaper = practiceDubboService.create(module.getId(), ARENA_QCOUNT, SubjectType.SUBJECT_GONGWUYUAN);
        final String roomName = "竞技-" + module.getName();
        int delta = RandomUtils.nextInt(1,3);
        final ValueOperations valueOperations = arenaRedisTemplate.opsForValue();
        final String roomIdKey = RedisArenaKeys.getRoomIdKey();

        if (!arenaRedisTemplate.hasKey(roomIdKey)) {//初始化id
            valueOperations.set(roomIdKey,"23448564");
        }
        final Long id = valueOperations.increment(roomIdKey, delta);
        final ArenaRoom arenaRoom = ArenaRoom.builder()
                .createTime(System.currentTimeMillis())
                .id(id)
                .maxPlayerCount(playerCount)
                .module(roomName)
                .name(roomName)
                .time(ARENA_LIMIT_TIME)
                .practicePaper(practicePaper)
                .qcount(practicePaper.getQcount())
                .status(ArenaRoomStatus.CREATED)
                .players(new ArrayList<Long>())
                .practices(new ArrayList<Long>())
                .build();
        arenaRoomDao.insert(arenaRoom);
        //把最新的放到第一位
        arenaRedisTemplate.opsForList().leftPush(RedisArenaKeys.getOnlineRoomListKey(),arenaRoom.getId());
        return arenaRoom;
    }


    public ArenaRoom findById(int id) {
        return arenaRoomDao.findById(id);
    }
}
