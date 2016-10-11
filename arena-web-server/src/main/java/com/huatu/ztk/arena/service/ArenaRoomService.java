package com.huatu.ztk.arena.service;

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


    public ArenaRoom findById(long id) {
        return arenaRoomDao.findById(id);
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
        if (arenaRoom.getPlayerIds().size() <= arenaRoom.getResults().size()) {
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
     * @return
     */
    public List<UserArenaRecord> findTodayRank() {
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

}
