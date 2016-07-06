package com.huatu.ztk.arena.service;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.ArenaResult;
import com.huatu.ztk.arena.bean.ArenaRoom;
import com.huatu.ztk.arena.bean.ArenaRoomStatus;
import com.huatu.ztk.arena.bean.ArenaRoomSummary;
import com.huatu.ztk.arena.common.ArenaErrors;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.commons.*;
import com.huatu.ztk.commons.spring.BizException;
import com.huatu.ztk.commons.spring.CommonErrors;
import com.huatu.ztk.commons.spring.ErrorResult;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.api.PracticeDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import com.huatu.ztk.paper.bean.PracticeCard;
import com.huatu.ztk.paper.bean.PracticePaper;
import com.huatu.ztk.paper.common.AnswerCardType;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    //保持的最大房间数量
    public static final int MAX_FREE_ROOM_COUNT = 50;
    //房间玩家个数策略
    public static final int[] PLAYER_COUNTS = new int[]{2,4,8};


    public static final ThreadPoolExecutor create_room_thread_pool = new ThreadPoolExecutor(1, 2, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2),new ThreadPoolExecutor.DiscardPolicy());

    @Resource(name = "arenaRedisTemplate")
    private RedisTemplate<String,String> arenaRedisTemplate;

    @Autowired
    private ArenaRoomDao arenaRoomDao;

    @Autowired
    private PracticeDubboService practiceDubboService;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

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
                operations.opsForZSet().size(RedisArenaKeys.getRoomFreePlayersKey());//房间总数量
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

        //房间人员已满,不能加入
        if (arenaRoom.getPlayers().size() >= arenaRoom.getMaxPlayerCount()) {
            throw new BizException(ArenaErrors.ROOM_NO_FREE_SEAT);
        }

        //更新redis数据
        arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                //设置用户存在的房间号
                final String roomIdStr = roomId + "";
                operations.opsForValue().set(userRoomKey, roomIdStr);
                //在线人数+1
                operations.opsForValue().increment(RedisArenaKeys.getArenaOnlineCount(),1);
                final String roomFreePlayersKey = RedisArenaKeys.getRoomFreePlayersKey();
                //重新设置有效人数
                operations.opsForZSet().add(roomFreePlayersKey,roomIdStr,arenaRoom.getPlayers().size());
                return null;
            }
        });


        arenaRoom.getPlayers().add(uid);//添加用户到房间
        //更新房间数据
        arenaRoomDao.save(arenaRoom);
        logger.info("userId={} join room roomId = {} success",uid,roomId);
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

        //删除用户记录
        arenaRoom.getPlayers().remove(uid);

        arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                //删除用户房间
                operations.delete(userRoomKey);
                //在线人数减一
                operations.opsForValue().increment(RedisArenaKeys.getArenaOnlineCount(),-1);

                final String roomFreePlayersKey = RedisArenaKeys.getRoomFreePlayersKey();
                //退出房间,重新设置有效人数
                operations.opsForZSet().add(roomFreePlayersKey,roomId+"",arenaRoom.getPlayers().size());
                return null;
            }
        });


        arenaRoomDao.save(arenaRoom);
        return arenaRoom;
    }


    /**
     * 用户调用开始PK的方法
     * @param roomId 房间id
     * @param uid 用户id
     * @throws BizException
     */
    public void startPk(long roomId,long uid,int terminal) throws BizException {
        final ArenaRoom arenaRoom = arenaRoomDao.findById(roomId);
        if (arenaRoom == null) {
            throw new BizException(ArenaErrors.ROOM_NOT_EXIST);
        }
        //用户所在索引位置
        final int userIndex = arenaRoom.getPlayers().indexOf(uid);
        if (userIndex <0) {//该用户不在该房间内,无权限开始竞技
            throw new BizException(CommonErrors.PERMISSION_DENIED);
        }

        if (arenaRoom.getStatus() == ArenaRoomStatus.FINISHED) {//已经结束的,则不需要处理
            return;
        }

        if (arenaRoom.getPlayers().size() == 1) {//没有足够的玩家,不能开始游戏
            throw new BizException(ArenaErrors.NOT_ENOUGH_PLAYER);
        }

        //TODO 此处应该做并发控制
        //正在进行状态
        arenaRoom.setStatus(ArenaRoomStatus.RUNNING);
        final PracticeCard practiceCard = practiceCardDubboService.create(arenaRoom.getPracticePaper(), terminal, AnswerCardType.ARENA_PAPER, uid);
        //设置该用户的试卷id
        arenaRoom.getPractices().add(userIndex,practiceCard.getId());
        arenaRoomDao.save(arenaRoom);

        logger.info("user start pk. roomId = {}, uid = {}",roomId, uid);
        arenaRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                final ZSetOperations zSetOperations = operations.opsForZSet();
                final String roomFreePlayersKey = RedisArenaKeys.getRoomFreePlayersKey();
                //pk开始,则从空闲房间列表移除
                zSetOperations.remove(roomFreePlayersKey,roomId+"");
                //添加到进行中的房间集合set
                operations.opsForSet().add(RedisArenaKeys.getOngoingRoomList(),roomId+"");
                return null;
            }
        });

        //异步触发调用新接口,创建新的房间
        create_room_thread_pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    //自动创建新新房间,来补充已经占用的房间
                    autoCreateRooms();
                }catch (Throwable e){
                    logger.error("ex",e);
                }
            }
        });

    }

    /**
     * 自动创建新新房间,来补充已经占用的房间
     */
    private synchronized void autoCreateRooms() {

        final String roomFreePlayersKey = RedisArenaKeys.getRoomFreePlayersKey();
        final ZSetOperations<String, String> zSetOperations = arenaRedisTemplate.opsForZSet();
        final Long size = zSetOperations.size(roomFreePlayersKey);
        //需要新建房间数量
        long newRoomCount = 0;
        if (size == null || size < MAX_FREE_ROOM_COUNT) {
            newRoomCount = 50 - size;
        }
        for (int i = 0; i < newRoomCount; i++) {
            int count = PLAYER_COUNTS[RandomUtils.nextInt(0,PLAYER_COUNTS.length)];
            create(count);
        }

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
        int delta = RandomUtils.nextInt(1,4);
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
        //添加新房间,score为房间可容纳的人数
        arenaRedisTemplate.opsForZSet().add(RedisArenaKeys.getRoomFreePlayersKey(),arenaRoom.getId()+"",arenaRoom.getMaxPlayerCount());
        return arenaRoom;
    }


    public ArenaRoom findById(long id) {
        return arenaRoomDao.findById(id);
    }

    /**
     * 分页查询信息
     * @param cursor
     * @return
     */
    public PageBean findForPage(long cursor) {
        final ZSetOperations<String, String> zSetOperations = arenaRedisTemplate.opsForZSet();
        long start = 0;
        if (cursor > 0) {//redis list range 包含最后一个元素
            start = cursor + 1;
        }
        long end = start + 19;
        //新游标
        long newCursor = end;
        final Set<String> list = zSetOperations.range(RedisArenaKeys.getRoomFreePlayersKey(), start, end);
        List<Long> roomIds = new ArrayList<>(list.size());
        for (String str : list) {//遍历转换为long
            final Long roomId = Longs.tryParse(str);
            if (roomId != null) {
                roomIds.add(roomId);
            }
        }
        List<ArenaRoom> rooms = arenaRoomDao.findByIds(roomIds);
        if (rooms.size() == 0) {//如果没数据,则用老的游标
            newCursor = cursor;
        }
        return new PageBean(rooms,newCursor,-1);
    }

    /**
     * 智能加入房间
     * @param uid 用户id
     * @return
     * @throws BizException
     */
    public ArenaRoom smartJoin(long uid) throws BizException {
        final String roomFreePlayersKey = RedisArenaKeys.getRoomFreePlayersKey();
        final ZSetOperations<String, String> zSetOperations = arenaRedisTemplate.opsForZSet();
        //直接从score 为1 的开始取,防止取到空间人数为0的房间
        final Set<String> roomIds = zSetOperations.rangeByScore(roomFreePlayersKey, 1, 10);
        ArenaRoom arenaRoom = null;
        for (String roomId : roomIds) {//房间列表
            try {
                logger.info("uid={} try join roomId={}",uid,roomId);
                arenaRoom = joinRoom(Long.valueOf(roomId), uid);
                if (arenaRoom != null) {//已经进入房间,则跳出循环
                    break;
                }
            }catch (BizException e){
                logger.warn("start join roomId={} fail.",roomId,e);
                final ErrorResult errorResult = e.getErrorResult();
                //用户已经在房间内,则不能再加入房间
                if (errorResult.getCode() == ArenaErrors.USER_IN_ROOM.getCode()) {
                    throw e;
                }
            }
        }

        if (arenaRoom == null) {//还为空,则进入房间失败
            throw new BizException(ArenaErrors.JOIN_ROOM_FAIL);
        }
        return arenaRoom;
    }

    /**
     * 根据练习id查询其所属的竞技场
     * @param practiceId
     * @return
     */
    public ArenaRoom findByPracticeId(long practiceId){
        final ArenaRoom arenaRoom = arenaRoomDao.findByPracticeId(practiceId);
        return arenaRoom;
    }


    /**
     * 添加新的竞技结果
     * @param id
     */
    public void addArenaResult(long id) {
        AnswerCard answerCard = practiceCardDubboService.findById(id);
        if (answerCard.getType() != AnswerCardType.ARENA_PAPER) {//只处理竞技场的答题卡
            return;
        }

        final long practiceId = answerCard.getId();

        //查询该练习对应的竞技场房间
        final ArenaRoom arenaRoom = findById(practiceId);
        if (arenaRoom == null) {
            logger.error("practiceId={} not find it`s arean room.");
            return;
        }


        List<ArenaResult> results = arenaRoom.getResults();
        //初始化结果集合,防止空指针异常
        if (results == null) {
            results = arenaRoom.getResults();
        }
        final long uid = answerCard.getUserId();
        final ArenaResult arenaResult = ArenaResult.builder()
                .elapsedTime(answerCard.getExpendTime())
                .rcount(answerCard.getRcount())
                .uid(uid)
                .build();

        //遍历已有结果,防止重复处理
        for (ArenaResult result : results) {
            if (result.getUid() == uid) {
                logger.warn("practiceId={} is in ArenaRoom results,so skip it.");
                break;
            }
        }

        //添加新的竞技结果
        results.add(arenaResult);

        //对结果做排序,保证排名是有顺序的
        Collections.sort(results, new Comparator<ArenaResult>() {
            @Override
            public int compare(ArenaResult o1, ArenaResult o2) {
                //默认正序排
                long sub = o2.getRcount() - o1.getRcount();
                if (sub != 0) {
                    return (int) sub;
                }

                long subTime = o1.getElapsedTime() - o2.getElapsedTime();
                return (int) subTime;
            }
        });

        //更新竞技排名
        arenaRoom.setResults(results);
        arenaRoomDao.save(arenaRoom);
        logger.info("add arena result roomId={}, data={}",arenaRoom.getId(), JsonUtil.toJson(arenaResult));
    }
}
