package com.huatu.ztk.arena.service;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.huatu.ztk.arena.bean.*;
import com.huatu.ztk.arena.common.ArenaRoomType;
import com.huatu.ztk.arena.common.RedisArenaKeys;
import com.huatu.ztk.arena.dao.ArenaRoomDao;
import com.huatu.ztk.arena.dao.UserArenaRecordDao;
import com.huatu.ztk.commons.*;
import com.huatu.ztk.paper.api.PracticeCardDubboService;
import com.huatu.ztk.paper.api.PracticeDubboService;
import com.huatu.ztk.paper.bean.AnswerCard;
import com.huatu.ztk.paper.bean.PracticePaper;
import com.huatu.ztk.paper.common.AnswerCardType;
import com.huatu.ztk.user.bean.UserDto;
import com.huatu.ztk.user.dubbo.UserDubboService;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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
    public static final int ARENA_LIMIT_TIME = 60*16;
    public static final int TODAY_MAX_RANK_COUNT = 20;

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
                .time(ARENA_LIMIT_TIME)
                .practicePaper(practicePaper)
                .qcount(practicePaper.getQcount())
                .status(ArenaRoomStatus.CREATED)
                .playerIds(new ArrayList<Long>())
                .practices(new ArrayList<Long>())
                .build();
        arenaRoom.setCreateTime(System.currentTimeMillis());
        arenaRoom.setId(id);
        arenaRoom.setType(type);
        arenaRoom.setModule(roomName);
        arenaRoom.setName(roomName);
        arenaRoomDao.insert(arenaRoom);
        return arenaRoom;
    }


    public ArenaRoom findById(long roomId, long id) {
        final ArenaRoom arenaRoom = arenaRoomDao.findById(roomId);
        if (arenaRoom == null) {
            return arenaRoom;
        }
        // TODO: 10/11/16 1:此处设置玩家人数 2:权限判断
//        arenaRoom.setPlayers();
        return arenaRoom;
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
        List<ArenaResult> results = Optional.of(arenaRoom.getResults()).orElse(new ArrayList<>());

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
        //更新竞技排名
        arenaRoom.setResults(results);

        //答题结果已经够数,说明都已经交卷
        if (arenaRoom.getResults().size()>=arenaRoom.getPlayerIds().size()) {
            //状态设置为已完成
            arenaRoom.setStatus(ArenaRoomStatus.FINISHED);
            int maxRcount = -1;
            ArenaResult winner = null;//胜者id
            for (ArenaResult result : results) {//遍历答题结果,获取计算出胜者id
                if (result.getRcount() > maxRcount) {//正确数量多,取新的
                    maxRcount = result.getRcount();
                    winner = result;
                }else if (result.getRcount() == maxRcount) {//两人答对数量一样
                    if (result.getElapsedTime() < winner.getElapsedTime()) {//当两人答对数量一致,那么用时短的获胜
                        winner = result;
                    }
                }
            }
            //设置胜者id
            arenaRoom.setWinner(winner.getUid());
        }
        arenaRoomDao.save(arenaRoom);

        //删除用户的房间状态
        redisTemplate.delete(RedisArenaKeys.getUserRoomKey(uid));
        logger.info("add arena result roomId={}, data={}",arenaRoom.getId(), JsonUtil.toJson(arenaResult));
        //更新用户竞技记录
        updateUserArenaRecord(arenaRoom.getId(),answerCard,userDto);

        //所有的竞技结果已经处理完,需要对第一名进行胜场+1
        if (arenaRoom.getStatus() == ArenaRoomStatus.FINISHED) {
            final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
            //第一名的用户胜利场次+1
            final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(System.currentTimeMillis(), "yyyymmdd"));
            zSetOperations.incrementScore(arenaDayRankKey, arenaRoom.getWinner() +"",1);
            redisTemplate.expire(arenaDayRankKey,20, TimeUnit.DAYS);//记录20天有效
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
    }

    /**
     * 分页查询我的竞技记录
     * @param uid 用户id
     * @param cursor 游标
     * @return
     */
    public PageBean<ArenaRoomSimple> history(long uid, long cursor) {
        // TODO: 10/11/16
        return null;
    }

    /**
     * 查询今日排行
     *
     * @return
     */
    public List<UserArenaRecord> findTodayRank() {
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        final String arenaDayRankKey = RedisArenaKeys.getArenaDayRankKey(DateFormatUtils.format(System.currentTimeMillis(), "yyyymmdd"));
        final Set<String> strings = zSetOperations.reverseRange(arenaDayRankKey, 0, TODAY_MAX_RANK_COUNT-1);
        List<UserArenaRecord> records = Lists.newArrayList();
        for (String uidStr : strings) {
            //获胜场数
            final int winCount = zSetOperations.score(arenaDayRankKey, uidStr).intValue();
            final UserDto userDto = userDubboService.findById(Long.valueOf(uidStr));
            final Player player = Player.builder()
                    .uid(userDto.getId())
                    .avatar(userDto.getAvatar())
                    .nick(userDto.getNick())
                    .build();
            final UserArenaRecord arenaRecord = UserArenaRecord.builder()
                    .uid(userDto.getId())
                    .player(player)
                    .winCount(winCount)
                    .build();
            records.add(arenaRecord);
        }

        //对排行做倒叙排
        records.sort(new Comparator<UserArenaRecord>(){
            @Override
            public int compare(UserArenaRecord o1, UserArenaRecord o2) {
                return o2.getWinCount() - o1.getWinCount();//倒序排
            }
        });

        return records;
    }

}
