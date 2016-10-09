package com.huatu.ztk.arena.service;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.*;
import com.huatu.ztk.arena.common.ArenaErrors;
import com.huatu.ztk.arena.common.ArenaRoomType;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dao.UserArenaRecordDao;
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
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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
    public static final int ARENA_LIMIT_TIME = 60*16;

    @Resource(name = "redisTemplate")
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private ArenaRoomDao arenaRoomDao;

    @Autowired
    private PracticeDubboService practiceDubboService;

    @Autowired
    private PracticeCardDubboService practiceCardDubboService;

    @Autowired
    private UserArenaRecordDao userArenaRecordDao;

    @Autowired
    private UserDubboService userDubboService;

    /**
     * 查询竞技场汇总信息
     * @return
     */
    public ArenaRoomSummary summary() {
        final SetOperations<String,String> setOperations = redisTemplate.opsForSet();
        final ValueOperations<String,String> valueOperations = redisTemplate.opsForValue();
        final String countStr = valueOperations.get(RedisArenaKeys.ARENA_ONLINE_COUNT);//在线人数
        final Long allRoomCount = redisTemplate.opsForZSet().size(RedisArenaKeys.getRoomFreePlayersKey());//房间总数量
        final Long ongoingRoomCount = setOperations.size(RedisArenaKeys.ONGOING_ROOM_LIST);//正在考试的房间

        final Integer palyerCount = Integer.valueOf(countStr);
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
     * 随机创建一个房间
     * @return
     */
    public ArenaRoom create(Integer moduleId, int playerCount){
        int type = ArenaRoomType.RANDOM_POINT;//默认是随机知识点
        String roomName = "竞技-综合知识点";

        final Module module = ModuleConstants.getModuleById(moduleId);
        if (module != null) {
            roomName = "竞技-"+module.getName();
            type = ArenaRoomType.SPECIFIED_POINT;
        }else {//查询不到,说明是随机知识点
            //随机选取模块
            moduleId = ModuleConstants.GOWUYUAN_MODULE_IDS.get(RandomUtils.nextInt(0,ModuleConstants.GOWUYUAN_MODULE_IDS.size()));
        }

        final PracticePaper practicePaper = practiceDubboService.create(SubjectType.SUBJECT_GONGWUYUAN,moduleId,ARENA_QCOUNT);
        final ValueOperations valueOperations = redisTemplate.opsForValue();
        final String roomIdKey = RedisArenaKeys.getRoomIdKey();

        if (!redisTemplate.hasKey(roomIdKey)) {//初始化id
            valueOperations.set(roomIdKey,"23448564");
        }

        int delta = RandomUtils.nextInt(1,4);//随机步长
        final Long id = valueOperations.increment(roomIdKey, delta);
        final ArenaRoom arenaRoom = ArenaRoom.builder()
                .createTime(System.currentTimeMillis())
                .id(id)
                .type(type)
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
        return arenaRoom;
    }

    /**
     * 查询用户竞技记录,如果不存在,则初始化用户它
     * @param uid
     * @return
     */
    public UserArenaRecord findAndInit(long uid){
        UserArenaRecord userArenaRecord = userArenaRecordDao.findByUid(uid);
        //用户还没有竞技记录,则创建新的
        if (userArenaRecord == null) {
            final UserDto userDto = userDubboService.findById(uid);
            userArenaRecord = UserArenaRecord.builder()
                    .arenas(new ArrayList<>())
                    .arenaCount(0)
                    .nick(userDto.getNick())
                    .avgScore(0)
                    .uid(uid)
                    .build();
        }

        return userArenaRecord;
    }


    public ArenaRoom findById(long id) {
        return arenaRoomDao.findById(id);
    }

    /**
     * 分页查询信息
     * @param cursor
     * @param type
     * @return
     */
    public PageBean findForPage(long cursor, int type) {
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
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

        if (start == 0) {//第一次请求,则把正在进行的也加入进来
            //取出所有正在竞技的房间
            final Set<String> members = redisTemplate.opsForSet().members(RedisArenaKeys.getOngoingRoomList());
            for (String member : members) {
                final Long roomId = Longs.tryParse(member);
                if (roomId != null) {
                    roomIds.add(roomId);
                }
            }
        }
        List<ArenaRoom> rooms = arenaRoomDao.findByIds(roomIds);
        if (rooms.size() == 0) {//如果没数据,则用老的游标
            newCursor = cursor;
        }

        if (-1 != type) {//传入的是指定类型,则查询
            List<ArenaRoom> tmp = new ArrayList<>();
            for (ArenaRoom room : rooms) {//遍历房间列表,找出指定的房间
                if (room.getType() == type) {
                    tmp.add(room);
                }
            }
            rooms = tmp;
        }

        return new PageBean(rooms,newCursor,-1);
    }

    /**
     * 添加新的竞技结果
     * @param id
     */
    public void addArenaResult(long id) {
        AnswerCard answerCard = practiceCardDubboService.findById(id);
        if (answerCard == null) {
            logger.error("practiceId={} not exist",id);
            return;
        }

        if (answerCard.getType() != AnswerCardType.ARENA_PAPER) {//只处理竞技场的答题卡
            return;
        }

        final long practiceId = answerCard.getId();

        //查询该练习对应的竞技场房间
        final ArenaRoom arenaRoom = arenaRoomDao.findByPracticeId(practiceId);
        if (arenaRoom == null) {
            logger.error("practiceId={} not find it`s arean room.");
            return;
        }

        //竞技结果
        List<ArenaResult> results = arenaRoom.getResults();
        //初始化结果集合,防止空指针异常
        if (results == null) {
            results = new ArrayList<>();
        }
        final long uid = answerCard.getUserId();
        //遍历已有结果,防止重复处理
        for (ArenaResult result : results) {
            if (result.getUid() == uid) {//已经处理过的,不需要再进行处理
                logger.warn(" practiceId={} is in ArenaRoom results,so skip it.");
                return;
            }
        }
        final UserDto userDto = userDubboService.findById(uid);
        final ArenaResult arenaResult = ArenaResult.builder()
                .elapsedTime(answerCard.getExpendTime())
                .rcount(answerCard.getRcount())
                .nick(userDto.getNick())
                .uid(uid)
                .build();



        //添加新的竞技结果
        results.add(arenaResult);

        //对结果做排序,保证排名是有顺序的,排名算法:分数优先,分数一样的情况下,耗时短的靠前
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
        //状态设置为已完成
        arenaRoom.setStatus(ArenaRoomStatus.FINISHED);
        arenaRoomDao.save(arenaRoom);

        //从正在进行的房间移除
        redisTemplate.opsForSet().remove(RedisArenaKeys.getOngoingRoomList(),arenaRoom.getId()+"");
        //删除用户的房间状态
        redisTemplate.delete(RedisArenaKeys.getUserRoomKey(uid));

        logger.info("add arena result roomId={}, data={}",arenaRoom.getId(), JsonUtil.toJson(arenaResult));


        //更新用户竞技记录
        updateUserArenaRecord(arenaRoom.getId(),answerCard,userDto);

        //TODO 需要注意的是,有的用户参加了比赛,但是没有提交试卷,这样的话,会导致有的场次没有添加排名数据
        //所有的竞技结果已经处理完,需要对第一名进行胜场+1
        if (arenaRoom.getPlayers().size() <= arenaRoom.getResults().size()) {
            final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
            //第一名的用户胜利场次+1
            final long winUid = arenaRoom.getResults().get(0).getUid();
            zSetOperations.incrementScore(RedisArenaKeys.getArenaRankKey(), winUid +"",1);

            final UserArenaRecord userArenaRecord = userArenaRecordDao.findByUid(winUid);
            //第一名胜场+1
            userArenaRecord.setWinCount(userArenaRecord.getWinCount()+1);
            //更新用户胜场信息
            userArenaRecordDao.save(userArenaRecord);
        }
    }

    /**
     * 更新用户竞技记录
     * @param arenaId
     * @param answerCard
     * @param userDto
     */
    private void updateUserArenaRecord(long arenaId,AnswerCard answerCard,UserDto userDto){
        logger.info("update userId={} UserArenaRecord,arenaId={}",answerCard.getId(),arenaId);
        final long uid = answerCard.getUserId();
        UserArenaRecord userArenaRecord = findAndInit(uid);


        final List<Long> arenas = userArenaRecord.getArenas();
        if (arenas.indexOf(arenaId) > 0) {//已经处理过,不再处理
            logger.warn("arena record has proocess,skip this message. arenaId={},practiceId={},uid={}",arenaId,answerCard.getId(),uid);
            return;
        }
        //所有的分数,这种算法会损失一定的精度
        final double allScore = userArenaRecord.getAvgScore() * userArenaRecord.getArenaCount() + answerCard.getScore();

        //计算平均分
        double avgScore = new BigDecimal(allScore).divide(new BigDecimal(userArenaRecord.getArenaCount()+1),1, RoundingMode.HALF_UP).doubleValue();
        //每次都把最新的添加到第一位,方便分页查询的时候,倒序排
        arenas.add(0,arenaId);//添加新竞技场id
        userArenaRecord.setArenaCount(arenas.size());
        userArenaRecord.setAvgScore(avgScore);
        //每次更新时设置昵称
        userArenaRecord.setNick(userDto.getNick());
        userArenaRecord.setArenas(arenas);
        userArenaRecordDao.save(userArenaRecord);
    }

    /**
     * 分页查询我的竞技记录
     * @param uid 用户id
     * @param cursor 游标
     * @return
     */
    public PageBean<ArenaRoom> findMyArenas(long uid, long cursor) {
        cursor = Long.max(cursor,0);
        final UserArenaRecord userArenaRecord = findAndInit(uid);
        final List<Long> arenas = userArenaRecord.getArenas();
        long start = cursor;
        if (start >= arenas.size()) {//大于,说明没有新数据
            return new PageBean<ArenaRoom>(new ArrayList<>(),start,-1);
        }
        long end = Long.min(arenas.size(),start+20);
        final List<Long> roomIds = arenas.subList((int) start, (int) end);
        final List<ArenaRoom> arenaRooms = arenaRoomDao.findByIds(roomIds);

        //遍历房间,计算用户每个房间的排名情况
        for (ArenaRoom arenaRoom : arenaRooms) {
            final List<ArenaResult> results = arenaRoom.getResults();

            int myRank = arenaRoom.getPlayers().size()+1;//默认所有玩家最后一名
            if (arenaRoom.getType() == ArenaRoomStatus.FINISHED) {
                myRank = arenaRoom.getResults().size()+1;//已经完成的,则取所有交卷的最后一名+1
            }
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).getUid() == uid) {//本人答题卡
                    myRank = i+1;//本人答题卡的位置即为排名
                    break;
                }
            }
            //动态设置我的排名
            arenaRoom.setMyRank(myRank);
        }
        return new PageBean<ArenaRoom>(arenaRooms,end,-1);
    }

    /**
     * 查询排行榜
     * @return
     */
    public List<UserArenaRecord> findRank() {
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        //取前20条
        final Set<String> strings = zSetOperations.reverseRange(RedisArenaKeys.getArenaRankKey(), 0, 19);
        List<Long> uidList = new ArrayList<>(strings.size());
        for (String string : strings) {
            Long uid = Longs.tryParse(string);
            if (uid != null) {
                uidList.add(uid);
            }
        }
        //TODO 后期可以做一定的缓存
        return userArenaRecordDao.findByUids(uidList);
    }

    /**
     * 查询我的排行名次
     * @param uid
     * @return
     */
    public long findMyRank(long uid) {
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        Long rank = zSetOperations.reverseRank(RedisArenaKeys.getArenaRankKey(), uid + "");

        //如果还没有排行,则取具有排行的最后一名
        if (rank == null) {
            final Long size = zSetOperations.size(RedisArenaKeys.getArenaRankKey());
            rank = size+1;
        }else {
            rank = rank +1;//索引是从0开始的
        }
        return rank;
    }
}
